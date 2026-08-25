# Page Widget 설계

`DEVICE_PAGE` 안에 **카드에 무엇을 보여 줄지**만 저장합니다. 시간축 그래프는 위젯이 아닙니다.

> API prefix: `/api/manager/widgets`  
> DDL: [V018__create_page_widget.sql](../../sql/history/V018__create_page_widget.sql)  
> 페이지 매핑: [DEVICE_PAGE_API.md](./DEVICE_PAGE_API.md)

---

## 1. 개요

```text
DEVICE_PAGE
  ├─ device_page          그 화면의 장비
  └─ page_widget          카드 정의 (last / aggregate / count)
       └─ page_widget_point     포인트 1:N
```

차트는 `GET /api/manager/query/chart` 같은 조회 API로 따로 갑니다.

### 1.1 조회 종류

| `query_kind` | 하는 일 |
|--------------|---------|
| `last` | 지금 값 |
| `aggregate` | 한 숫자(또는 그룹 목록)로 줄이기 |
| `count` | 장비 수 (Influx 아님) |

### 1.2 행은 배포 전에 다 박아두지 않음

운영 중 POST로 추가합니다. 배포가 필요한 건 **새 연산·새 컬럼**을 백엔드에 넣을 때뿐입니다.

### 1.3 제약

| 항목 | 규칙 |
|------|------|
| `pageCodeId` | DEVICE_PAGE 그룹만 |
| UK | `(page_code_id, name)` |
| page 삭제 | FK RESTRICT |
| widget 삭제 | point CASCADE |

---

## 2. 테이블

**구현 상태:** DDL + Java CRUD ✅  조회 last/aggregate ⬜  chart API ⬜

### 2.1 `page_widget`

| 컬럼 | 타입 | NULL | 설명 |
|------|------|------|------|
| `id` | INT | N | PK |
| `page_code_id` | INT | N | DEVICE_PAGE `common_code.id` |
| `name` | VARCHAR(100) | N | 표시명 |
| `enabled` | TINYINT(1) | N | 기본 1 |
| `query_kind` | VARCHAR(16) | N | last / aggregate / count |
| `op` | VARCHAR(16) | Y | aggregate만. `delta_sum` / `weighted_avg` / `divide` |
| `group_by` | VARCHAR(16) | Y | `device` / `point` / `location` |
| `weight_point` | VARCHAR(100) | Y | weighted_avg 가중치 |
| `numerator_point` | VARCHAR(100) | Y | divide 분자 |
| `denominator_point` | VARCHAR(100) | Y | divide 분모 |
| `created_dt` | TIMESTAMP(6) | Y | |
| `updated_dt` | TIMESTAMP(6) | Y | |

### 2.2 `page_widget_point` — 1:N

| 컬럼 | 타입 | NULL | 설명 |
|------|------|------|------|
| `id` | INT | N | PK |
| `widget_id` | INT | N | FK CASCADE |
| `point_name` | VARCHAR(100) | N | Influx `point_name` |

UK: `(widget_id, point_name)`

역할 포인트(`weight_point` 등)는 이 테이블이 아니라 위젯 컬럼입니다.

구 PDU `path`는 **`location_node`**. `group_by=location`.

---

## 3. 레코드 예시

**dashboard — 칠러**

```json
{
  "pageCode": "dashboard",
  "name": "칠러",
  "queryKind": "last",
  "pointNames": ["status", "W", "in_temp", "out_temp"]
}
```

**dashboard — PF**

```json
{
  "pageCode": "dashboard",
  "name": "PF",
  "queryKind": "aggregate",
  "op": "weighted_avg",
  "weightPoint": "W",
  "pointNames": ["PF"]
}
```

---

## 4. 조회와의 관계 (예정)

위젯 CRUD는 정의만 줍니다. 카드 값은 last / aggregate, 그래프는 chart API입니다.

```http
GET /api/manager/widgets?pageCode=dashboard
GET /api/manager/query/last?pageCode=dashboard&pointNames=status,W
GET /api/manager/query/chart?pageCode=dashboard&pointNames=W&window=15m&compare=yesterday,prevMonth
```

---

## 5. 위젯 CRUD API

**구현 상태:** ✅

| Method | Path | 상태 |
|--------|------|------|
| GET | `/api/manager/widgets?pageCode=dashboard` | ✅ |
| GET | `/api/manager/widgets/{id}` | ✅ |
| POST | `/api/manager/widgets` | ✅ |
| PUT | `/api/manager/widgets/{id}` | ✅ |
| DELETE | `/api/manager/widgets/{id}` | ✅ |

### 5.1 목록 — `GET /api/manager/widgets?pageCode=`

`pageCode` 필수. `id` 오름차순. 비활성 위젯도 포함.

| 조건 | HTTP | 메시지 |
|------|------|--------|
| pageCode 없음 | 400 | Required parameter 'pageCode' ... |
| DEVICE_PAGE 코드 없음 | 404 | `DEVICE_PAGE code not found: {code}` |

### 5.2 등록 — `POST /api/manager/widgets`

```json
{
  "pageCode": "dashboard",
  "name": "칠러",
  "enabled": true,
  "queryKind": "last",
  "pointNames": ["status", "W"]
}
```

`enabled` 생략 시 true.

| 조건 | HTTP | 메시지 |
|------|------|--------|
| DEVICE_PAGE 코드 없음 | 404 | `DEVICE_PAGE code not found: {code}` |
| queryKind 아님 | 400 | `queryKind must be last, aggregate, or count` |
| aggregate 인데 op 없음 | 400 | `op is required for aggregate` |
| 같은 페이지에 이름 중복 | 409 | `widget name already exists on this page` |

### 5.3 수정 — `PUT /api/manager/widgets/{id}`

페이지는 바꾸지 않습니다. `name`, `queryKind` 필수. `enabled` 생략 시 기존 값 유지. 포인트는 요청 목록으로 교체합니다.

### 5.4 삭제 — `DELETE /api/manager/widgets/{id}`

삭제된 id를 돌려줍니다. point도 같이 지웁니다.

---

## 6. 하지 않음

- 화면 좌표·sort_order
- spec JSON 컬럼
- 시간축 차트 위젯 (`query_kind=chart`)
- PUE·PF common_code
- 위젯 이름별 조회 메서드
- 구 `/dashboard/pdu/*` path
- 측정값 저장
- 배포 시 위젯 시드 필수
