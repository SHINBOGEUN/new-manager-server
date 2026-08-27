# Page Widget 설계

`DEVICE_PAGE` 안에 **카드(위젯)** 를 두고, 카드가 조회 정의를 가집니다.  
페이지를 고르면 위젯 목록이 나오고, 종류(`query_kind`)에 따라 값을 조회합니다.

> API prefix: `/api/manager/widgets`  
> DDL: [V018__create_page_widget.sql](../../sql/history/V018__create_page_widget.sql)  
> count 옵션 컬럼: [V019__page_widget_count_mode.sql](../../sql/history/V019__page_widget_count_mode.sql)

```text
common_code (DEVICE_PAGE)          ← 페이지
  └─ page_widget                   ← 카드 (공통 + kind별 NULL 옵션 컬럼)
       ├─ page_widget_device       ← 장비 1:N (last / aggregate)
       ├─ page_widget_point        ← 포인트 1:N (last / aggregate)
       └─ page_widget_layout       ← 2D 배치 1:1 (선택)
```

---

## 설계 원칙 (합의)

| 선택 | 이유 |
|------|------|
| `page_widget`에 kind 옵션을 **소수 NULL 컬럼**으로 둠 | JSON / kind별 테이블 모두 비선호 |
| `query_kind`는 **Java enum**이 소스 오브 트루스 | DB `common_code`로 동작 타입을 관리하지 않음 |
| 목록성 데이터만 자식 테이블 | `device` / `point` / `layout` |
| 필수 규칙 검증 | 도메인 `PageWidget.validateOptions()` / `validateBindings()` |

kind가 늘어나면 **그때 필요한 컬럼만** `page_widget`에 추가합니다.  
미리 JSON·config 테이블로 확장하지 않습니다.

---

## 1. 조회 종류

| `query_kind` | 하는 일 | 조회 API |
|--------------|---------|----------|
| `last` | 지정 장비의 최신 값 | `GET /query/last?widgetId=` ✅ |
| `count` | **전체 enabled 장비** 개수 (모드별) | `GET /query/count?widgetId=` ✅ |
| `aggregate` | 지정 장비로 집계 | 예정 (PUE 후순위) |

차트는 위젯이 아님 → `GET /api/manager/query/chart` (예정)

---

## 2. kind별 필수 / 금지 (정의)

**검증 위치:** `PageWidget` 도메인 (`create` / `update` 시).  
DTO `@NotEmpty`로 kind 공통 필수를 걸지 않음.

| `query_kind` | 필수 | 선택 | 금지(넣으면 400) |
|--------------|------|------|------------------|
| **last** | `deviceIds` (≥1), `pointNames` (≥1) | `layout`, `groupBy` | `op`, `weightPoint`, `numeratorPoint`, `denominatorPoint`, `countMode`, `countModelId` |
| **aggregate** | `deviceIds` (≥1), `op` | `pointNames` (divide 제외 시 권장), `groupBy`, `layout` | `countMode`, `countModelId` |
| **count** | — (장비/포인트 불필요) | `countMode` (없으면 조회 시 `by_model`), `layout` | `op`, `weightPoint`, `numeratorPoint`, `denominatorPoint` (device/point는 무시·비권장) |

### aggregate `op`별 추가 필수

| `op` | 추가 필수 |
|------|-----------|
| `delta_sum` | — |
| `weighted_avg` | `weightPoint` |
| `divide` | `numeratorPoint`, `denominatorPoint` |

### count `countMode`별 추가 필수

| `countMode` | 의미 | 추가 필수 |
|-------------|------|-----------|
| `total` | enabled 장비 **총 개수만** | — |
| `by_model` | 총 개수 + **모델별** breakdown | — |
| `model` | **특정 모델** enabled 개수만 | `countModelId` |

`count` 조회 대상은 위젯에 묶인 장비가 아니라 **`devices` 테이블에서 `enabled=1` 인 전체**입니다.

---

## 3. 테이블

### 3.1 `page_widget`

| 컬럼 | 설명 |
|------|------|
| `page_code_id` | DEVICE_PAGE |
| `name` | 카드 표시명 (페이지 내 UK) |
| `enabled` | UI 표시 on/off |
| `query_kind` | last / aggregate / count |
| `op` | aggregate only |
| `group_by` | device / point / location (선택) |
| `weight_point` | weighted_avg |
| `numerator_point` / `denominator_point` | divide |
| `count_mode` | count only: total / by_model / model (V019) |
| `count_model_id` | count_mode=model 일 때 `device_model.id` (V019) |

### 3.2 `page_widget_device`

last / aggregate 용. **count 위젯은 비워 둠.**

### 3.3 `page_widget_point`

last / aggregate 용. count는 사용하지 않음.

### 3.4 `page_widget_layout` (2D, 1:1)

| 컬럼 | 설명 |
|------|------|
| `widget_id` | PK = `page_widget.id` |
| `grid_x`, `grid_y` | 그리드 좌표 (≥ 0) |
| `w`, `h` | 가로·세로 칸 (≥ 1) |

---

## 4. 예시

**last — 칠러**

```json
{
  "pageCode": "dashboard",
  "name": "칠러A",
  "queryKind": "last",
  "deviceIds": [40],
  "pointNames": ["tempIn", "tempOut", "W"],
  "layout": { "gridX": 0, "gridY": 0, "w": 16, "h": 8 }
}
```

**count — 전체**

```json
{
  "pageCode": "dashboard",
  "name": "장비 수",
  "queryKind": "count",
  "countMode": "total",
  "deviceIds": [],
  "pointNames": []
}
```

**count — 특정 모델**

```json
{
  "pageCode": "dashboard",
  "name": "APC PDU 수",
  "queryKind": "count",
  "countMode": "model",
  "countModelId": 10,
  "deviceIds": [],
  "pointNames": []
}
```

**count — 모델별**

```json
{
  "pageCode": "dashboard",
  "name": "모델별 장비 수",
  "queryKind": "count",
  "countMode": "by_model",
  "deviceIds": [],
  "pointNames": []
}
```

---

## 5. 조회

```http
GET /api/manager/widgets?pageCode=dashboard
GET /api/manager/query/last?widgetId=12
GET /api/manager/query/count?widgetId=12
```

| 조회 | 상태 | 문서 |
|------|------|------|
| last | ✅ | [QUERY_LAST_API.md](../query/QUERY_LAST_API.md) |
| count | ✅ | [QUERY_COUNT_API.md](../query/QUERY_COUNT_API.md) |
| aggregate | 예정 (PUE는 후순위) | — |

---

## 6. 위젯 CRUD

**구현 상태:** ✅

| Method | Path |
|--------|------|
| GET | `/api/manager/widgets?pageCode=` |
| GET | `/api/manager/widgets/{id}` |
| POST | `/api/manager/widgets` |
| PUT | `/api/manager/widgets/{id}` |
| PATCH | `/api/manager/widgets/{id}/enabled` |
| PUT | `/api/manager/widgets/{id}/layout` |
| DELETE | `/api/manager/widgets/{id}` |

- **last / aggregate:** `deviceIds` 필수. last는 `pointNames`도 필수.
- **count:** `deviceIds` / `pointNames` 생략 또는 `[]`. `countMode` (+ `model`이면 `countModelId`) 사용.
- `layout`은 생성 시 선택. update에서 `layout` 생략 시 기존 배치 유지.
