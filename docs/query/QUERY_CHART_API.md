# Query API (chart)

위젯에 정의된 **범위 + point**로 Influx 시계열을 조회합니다.

```http
GET /api/manager/widgets?pageCode=dashboard
GET /api/manager/query/chart?widgetId=12
GET /api/manager/query/chart?widgetId=12&rangePreset=today&window=15m&seriesMode=by_phase
```

관련 위젯 정의: [PAGE_WIDGET_API.md](../device/PAGE_WIDGET_API.md)  
DDL: [`18_page_widget_chart.sql`](../../sql/schema/18_page_widget_chart.sql), [`21_page_widget_model.sql`](../../sql/schema/21_page_widget_model.sql)

---

## 위젯 생성 (chart)

| 필드 | 필수 | 설명 |
|------|------|------|
| `queryKind` | ✅ | `"chart"` |
| `pointNames` | ✅ | 동일 unit 권장. phase면 `L1`,`L2`,`L3` |
| `chartScope` | | `devices`(기본) \| `models` |
| `deviceIds` | scope=devices | enabled 필터는 조회 시 |
| `modelIds` | scope=models | 해당 모델의 enabled 장비 전체 |
| `chartSeriesMode` | | `per_device`(기본) \| `sum` \| `by_phase` \| `by_path` |
| `chartRangePreset` | | `last_24h`(기본) \| `today` \| `yesterday` \| `last_7d` \| `this_month` \| `last_month` |
| `chartWindow` | | `1m` \| `5m`(기본) \| `15m` \| `1h` \| `1d` |

### seriesMode (UI 표기 ↔ API)

| UI (예: Power by Group) | API `chartSeriesMode` / `seriesMode` |
|-------------------------|--------------------------------------|
| **Total** | `sum` |
| **Phase** | `by_phase` |
| **Path** | `by_path` |
| **PDU** | `per_device` |

| 값 | 의미 |
|----|------|
| `per_device` | 장비(+point)마다 시리즈 1개 (PDU별) |
| `sum` | 동일 시각 버킷 값을 전부 합산 → 시리즈 1개 (Total) |
| `by_phase` | **point 이름**별 합산 (L1/L2/L3 등) |
### Path 설정 방법

1. `LOCATION_PATH` 공통코드 (시드: A/B/C) — 필요 시 코드 추가
2. **PDU(장비)** 에 `pathCodeId` 지정 (전원 피드 A/B/C…). Rack은 위치만, Path는 장비 단위
3. `by_path` 조회 시 **`devices.path_code_id`** 기준으로 그룹·합산 (location 조상과 무관)

예: 서로 다른 랙의 PDU라도 Path A이면 → 시리즈 `"A"` 하나로 합쳐짐.

### 예시 — 장비 여러 대

```json
{
  "pageCode": "dashboard",
  "name": "PDU 전력",
  "queryKind": "chart",
  "chartScope": "devices",
  "chartSeriesMode": "per_device",
  "chartRangePreset": "last_24h",
  "chartWindow": "5m",
  "deviceIds": [101, 102],
  "pointNames": ["W"]
}
```

### 예시 — 모델 범위 + phase

```json
{
  "pageCode": "dashboard",
  "name": "3상 합",
  "queryKind": "chart",
  "chartScope": "models",
  "chartSeriesMode": "by_phase",
  "chartRangePreset": "this_month",
  "chartWindow": "1h",
  "modelIds": [10],
  "deviceIds": [],
  "pointNames": ["L1", "L2", "L3"]
}
```

---

## `GET /api/manager/query/chart`

| 이름 | 필수 | 설명 |
|------|------|------|
| `widgetId` | ✅ | `queryKind=chart`, enabled |
| `rangePreset` | | 위젯 저장값 override |
| `window` | | 위젯 저장값 override |
| `seriesMode` | | 위젯 `chartSeriesMode` override (Total/Phase/Path/PDU 전환) |

조회 시 `seriesMode`를 넘기면 **위젯에 저장된 chartSeriesMode를 덮어씁니다** (DB 수정 없이 UI 드롭다운용).

### 응답 예

```json
{
  "status": 200,
  "data": {
    "widgetId": 12,
    "widgetName": "PDU 전력",
    "pageCode": "dashboard",
    "chartScope": "devices",
    "seriesMode": "by_phase",
    "rangePreset": "last_24h",
    "window": "5m",
    "start": "2026-08-26T02:00:00Z",
    "end": "2026-08-27T02:00:00Z",
    "unit": "W",
    "series": [
      {
        "key": "L1_WATT",
        "label": "L1_WATT",
        "deviceId": null,
        "pointName": "L1_WATT",
        "locationNodeCode": null,
        "times": ["2026-08-27T01:00:00Z", "2026-08-27T01:05:00Z"],
        "values": [120.5, 118.2]
      }
    ]
  }
}
```

`times[i]`와 `values[i]`는 같은 인덱스끼리 짝입니다.

### 오류

| 조건 | HTTP |
|------|------|
| widgetId 없음 | 400 |
| 위젯 없음 | 404 |
| queryKind ≠ chart | 400 |
| 위젯 disabled | 400 |
| point unit 불일치 | 400 |
| enabled 장비 > 200 | 400 |
| Influx 실패 | 503 |

### 미포함 (다음)

- 어제/전월 동일 구간 **비교 시리즈** (`compareMode`)
- 31일 vs 30일 월 정렬 정책
