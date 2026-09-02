# Page Widget 설계

`DEVICE_PAGE` 안에 **카드(위젯)** 를 두고, 카드가 조회 정의를 가집니다.  
페이지를 고르면 위젯 목록이 나오고, 종류(`query_kind`)에 따라 값을 조회합니다.

> API prefix: `/api/manager/widgets`  
> DDL: [`15_page_widget.sql`](../../sql/schema/15_page_widget.sql) ~ [`22_page_widget_layout.sql`](../../sql/schema/22_page_widget_layout.sql) (core + kind별 1:1 확장 + 바인딩)

```text
common_code (DEVICE_PAGE)          ← 페이지
  └─ page_widget                   ← 카드 (core: 공통 메타만)
       ├─ page_widget_aggregate    ← 1:1 (query_kind=aggregate)
       ├─ page_widget_count        ← 1:1 (query_kind=count)
       ├─ page_widget_chart        ← 1:1 (query_kind=chart)
       ├─ page_widget_device       ← 장비 1:N (last / aggregate / chart·devices)
       ├─ page_widget_model        ← 모델 1:N (chart·models)
       ├─ page_widget_point        ← 포인트 1:N (last / aggregate / chart)
       └─ page_widget_layout       ← 2D 배치 1:1 (선택)
```

---

## 설계 원칙 (합의)

| 선택 | 이유 |
|------|------|
| `page_widget`는 **core 8컬럼**만, kind 옵션은 **1:1 확장 테이블** | flat NULL 컬럼·JSON·범용 config 테이블 비선호 |
| `query_kind`는 **Java enum**이 소스 오브 트루스 | DB `common_code`로 동작 타입을 관리하지 않음 |
| 목록성 바인딩은 자식 테이블 | `device` / `model` / `point` / `layout` |
| 필수 규칙 검증 | 도메인 `PageWidget.validateOptions()` / `validateBindings()` |

kind가 늘어나면 **해당 kind용 1:1 확장 테이블**을 추가합니다 (`page_widget_*`).  
API JSON 필드명은 그대로 — 서버가 core + 확장을 조합해 응답/저장합니다.

---

## 1. 조회 종류

| `query_kind` | 하는 일 | 조회 API |
|--------------|---------|----------|
| `last` | 지정 장비의 최신 값 | `GET /query/last?widgetId=` ✅ |
| `count` | **전체 enabled 장비** 개수 (모드별) | `GET /query/count?widgetId=` ✅ |
| `chart` | 시계열 (장비/모델 범위 + point) | `GET /query/chart?widgetId=` ✅ |
| `aggregate` | 지정 장비로 집계 | ✅ [QUERY_AGGREGATE_API.md](../query/QUERY_AGGREGATE_API.md) |

차트는 위젯 `queryKind=chart` → [QUERY_CHART_API.md](../query/QUERY_CHART_API.md)

---

## 2. kind별 필수 / 금지 (정의)

**검증 위치:** `PageWidget` 도메인 (`create` / `update` 시).  
DTO `@NotEmpty`로 kind 공통 필수를 걸지 않음.

| `query_kind` | 필수 | 선택 | 금지(넣으면 400) |
|--------------|------|------|------------------|
| **last** | `deviceIds` (≥1), `pointNames` (≥1) | `layout`, `groupBy` | `op`, count/chart 옵션 |
| **aggregate** | `deviceIds` (≥1), `op` (usage\|power\|pue), `pointNames` (정확히 1개). pue는 `itDeviceIds`도 필수 | `aggregateRangePreset`, `groupBy`, `layout` | count/chart 옵션 |
| **count** | — | `countMode`, `layout` | `op`, chart 옵션, device/point 불필요 |
| **chart** | `pointNames` (≥1), scope에 따른 범위 | chart 옵션, `layout` | `op`, count 옵션 |

### chart 범위 (`chartScope`)

| scope | 필수 | 조회 시 |
|-------|------|---------|
| `devices` (기본) | `deviceIds` | 위젯 장비 중 enabled |
| `models` | `modelIds` (`page_widget_model`) | 해당 모델 enabled 장비 전체 |

### chart `chartSeriesMode`

| 값 | 의미 |
|----|------|
| `per_device` | 장비별 시리즈 |
| `sum` | 전체 합 1선 |
| `by_phase` | point 이름별 합 (L1/L2/L3) |
| `by_path` | location_node별 합 |

### aggregate preset (`op`)

| `op` | 계산 | 추가 필수 |
|------|------|-----------|
| `usage` | 선택 포인트 구간 차분 합 | `aggregateRangePreset` (미지정 시 `today`) |
| `power` | 선택 포인트 마지막값 합 | — (기간 기본 `last_24h`) |
| `pue` | total last / it last | `deviceIds`(total) + `itDeviceIds`(it), 겹침 불가, 동일 `pointNames` 1개 |

`pointNames` 예: `TOTAL_WT`, `W`, `TOTAL_KWH` — 모델 카탈로그 이름 그대로. `weightPoint` / `numeratorPoint` / `denominatorPoint`는 deprecated(항상 null).

### count `countMode`별 추가 필수

| `countMode` | 의미 | 추가 필수 |
|-------------|------|-----------|
| `total` | enabled 장비 **총 개수만** | — |
| `by_model` | 총 개수 + **모델별** breakdown | — |
| `model` | **특정 모델** enabled 개수만 | `countModelId` |

`count` 조회 대상은 위젯에 묶인 장비가 아니라 **`devices` 테이블에서 `enabled=1` 인 전체**입니다.

---

## 3. 테이블

### 3.1 `page_widget` (core)

| 컬럼 | 설명 |
|------|------|
| `id` | PK |
| `page_code_id` | DEVICE_PAGE (`common_code.id`) |
| `name` | 카드 표시명 (페이지 내 UK) |
| `enabled` | UI 표시 on/off |
| `query_kind` | last / aggregate / count / chart |
| `group_by` | device / point / location (선택, last·aggregate 표시용) |
| `created_dt` / `updated_dt` | 감사 타임스탬프 |

kind별 옵션 컬럼은 core에 두지 않습니다. 아래 **1:1 확장**에만 존재합니다.

### 3.2 kind별 1:1 확장 (`widget_id` = PK = FK → `page_widget.id`)

| 테이블 | `query_kind` | 컬럼 | API 필드 (동일) |
|--------|--------------|------|-----------------|
| `page_widget_aggregate` | aggregate | `op`, `range_preset` (`weight_*` 미사용) | `op`, `aggregateRangePreset` |
| `page_widget_count` | count | `count_mode`, `count_model_id` | `countMode`, `countModelId` |
| `page_widget_chart` | chart | `chart_scope`, `chart_series_mode`, `chart_range_preset`, `chart_window` | `chartScope`, `chartSeriesMode`, `chartRangePreset`, `chartWindow` |

- **last** — 확장 테이블 없음 (`group_by`만 core).
- 위젯 삭제 시 확장 행은 `ON DELETE CASCADE`.
- kind와 맞지 않는 확장 행은 도메인 검증에서 거부.

### 3.3 `page_widget_device`

last / aggregate / chart(scope=devices) 용. **count·chart(models)는 비움.**  
aggregate `pue`는 `device_role`: `total` / `it` (그 외·NULL = `default`).

### 3.4 `page_widget_model`

chart(scope=models) 용. `widget_id` + `model_id`.

### 3.5 `page_widget_point`

last / aggregate / chart 용. count는 사용하지 않음.

### 3.6 `page_widget_layout` (2D, 1:1)

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
| chart | ✅ | [QUERY_CHART_API.md](../query/QUERY_CHART_API.md) |
| aggregate | ✅ `GET /query/aggregate` | [QUERY_AGGREGATE_API.md](../query/QUERY_AGGREGATE_API.md) |

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

- **last / aggregate:** `deviceIds` 필수. `pointNames` 필수 (aggregate는 정확히 1개).
- **count:** `deviceIds` / `pointNames` 생략 또는 `[]`. `countMode` (+ `model`이면 `countModelId`) 사용.
- **chart:** `pointNames` 필수. `chartScope=devices`면 `deviceIds`, `models`면 `modelIds`. 상세 [QUERY_CHART_API.md](../query/QUERY_CHART_API.md).
- `layout`은 생성 시 선택. update에서 `layout` 생략 시 기존 배치 유지.
