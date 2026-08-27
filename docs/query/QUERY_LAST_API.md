# Query API (last)

위젯에 묶인 **장비 + 포인트**의 Influx 최신값을 조회합니다.

```http
GET /api/manager/widgets?pageCode=dashboard
GET /api/manager/query/last?widgetId=12
```

---

## `GET /api/manager/query/last`

| 이름 | 필수 | 설명 |
|------|------|------|
| `widgetId` | ✅ | `page_widget.id` (`queryKind=last`) |
| `lookbackHours` | | 기본 24, 최대 168 |

### 동작

1. 위젯 로드 (`queryKind=last`만)
2. `page_widget_device`의 **enabled** 장비
3. `page_widget_point`의 pointNames로 Influx `last()`
4. 장비 모델 SNMP point의 `unit`을 붙여 응답 (`unit` 없으면 null)
5. 응답에 `widgetName` + 장비별 points

### 응답 예

```json
{
  "status": 200,
  "data": {
    "widgetId": 12,
    "widgetName": "칠러A",
    "pageCode": "dashboard",
    "devices": [
      {
        "deviceId": 40,
        "deviceName": "Chiller-1",
        "locationNodeCode": "ZONE01",
        "locationNodeName": "냉각존",
        "deviceTypeCode": "CHILLER",
        "points": [
          { "pointName": "tempIn", "unit": "C", "value": 12.1, "time": "2026-08-26T01:00:00Z" },
          { "pointName": "W", "unit": "W", "value": 520.0, "time": "2026-08-26T01:00:00Z" }
        ]
      }
    ]
  }
}
```

값이 없는 장비/포인트는 `devices` / `points`에서 생략합니다.

### 오류

| 조건 | HTTP |
|------|------|
| widgetId 없음 | 400 |
| 위젯 없음 | 404 |
| queryKind ≠ last | 400 |
| lookbackHours 범위 밖 | 400 |
| Influx 실패 | 503 |
