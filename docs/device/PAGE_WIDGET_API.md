# Page Widget 설계

`DEVICE_PAGE` 안에 **카드(위젯)** 를 두고, 카드가 **장비 + 포인트**를 가집니다.  
페이지를 고르면 위젯 목록이 나오고, 각 위젯이 자기 device/point로 값을 조회합니다.

> API prefix: `/api/manager/widgets`  
> DDL: [V018__create_page_widget.sql](../../sql/history/V018__create_page_widget.sql)

```text
common_code (DEVICE_PAGE)          ← 페이지
  └─ page_widget                   ← 카드 (조회 정의)
       ├─ page_widget_device       ← 장비 1:N
       ├─ page_widget_point        ← 포인트 1:N
       └─ page_widget_layout       ← 2D 배치 1:1 (선택)
```

카드 조회·페이지 장비 필터 모두 **위젯에 묶인 device** (`page_widget_device`) 기준입니다.  
(`device_page` 테이블은 V018에서 삭제)

---

## 1. 조회 종류

| `query_kind` | 하는 일 |
|--------------|---------|
| `last` | 지정 장비의 지금 값 |
| `aggregate` | 지정 장비로 집계 (PUE 등) |
| `count` | 지정 장비 수 |

차트는 위젯이 아님 → `GET /api/manager/query/chart` (예정)

---

## 2. 테이블

### 2.1 `page_widget`

| 컬럼 | 설명 |
|------|------|
| `page_code_id` | DEVICE_PAGE |
| `name` | 카드 표시명 (페이지 내 UK) |
| `query_kind` | last / aggregate / count |
| `op` 등 | aggregate 옵션 |

### 2.2 `page_widget_device`

| 컬럼 | 설명 |
|------|------|
| `widget_id` | FK CASCADE |
| `device_id` | FK RESTRICT → `devices` |

UK: `(widget_id, device_id)`

### 2.3 `page_widget_point`

| 컬럼 | 설명 |
|------|------|
| `widget_id` | FK CASCADE |
| `point_name` | Influx `point_name` |

### 2.4 `page_widget_layout` (2D, 1:1)

| 컬럼 | 설명 |
|------|------|
| `widget_id` | PK = `page_widget.id` |
| `grid_x`, `grid_y` | 그리드 좌표 (≥ 0) |
| `w`, `h` | 가로·세로 칸 (≥ 1) |

드래그로 옮긴 위치를 저장. 없으면 `layout: null`. (3D는 추후 별도)

---

## 3. 예시

**칠러 카드 (장비 1대 + points)**

```json
{
  "pageCode": "dashboard",
  "name": "칠러A",
  "queryKind": "last",
  "deviceIds": [40],
  "pointNames": ["tempIn", "tempOut", "W"],
  "layout": { "gridX": 0, "gridY": 0, "w": 2, "h": 1 }
}
```

**PDU 여러 대**

```json
{
  "pageCode": "dashboard",
  "name": "PDU 실시간",
  "queryKind": "last",
  "deviceIds": [9, 10, 11],
  "pointNames": ["W", "PF", "AMP"]
}
```

---

## 4. 조회

```http
GET /api/manager/widgets?pageCode=dashboard
GET /api/manager/query/last?widgetId=12
```

| 조회 | 상태 | 문서 |
|------|------|------|
| last | ✅ | [QUERY_LAST_API.md](../query/QUERY_LAST_API.md) |
| aggregate | 예정 | — |
| count | 예정 | — |

---

## 5. 위젯 CRUD

**구현 상태:** ✅ (`deviceIds`, `layout` 포함)

| Method | Path |
|--------|------|
| GET | `/api/manager/widgets?pageCode=` |
| GET | `/api/manager/widgets/{id}` |
| POST | `/api/manager/widgets` |
| PUT | `/api/manager/widgets/{id}` |
| PUT | `/api/manager/widgets/{id}/layout` | 드래그 배치만 |
| DELETE | `/api/manager/widgets/{id}` |

`deviceIds` 필수. `last`는 `pointNames`도 필수.  
`layout`은 생성 시 선택. update에서 `layout` 생략 시 기존 배치 유지. 드래그는 `PUT .../layout` 권장.
