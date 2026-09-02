# Query API (count)

`devices` 테이블에서 **`enabled = 1`인 장비 전체**를 셉니다.  
위젯의 `page_widget_device`는 사용하지 않습니다. Influx를 사용하지 않습니다.

```http
GET /api/manager/widgets?pageCode=dashboard
GET /api/manager/query/count?widgetId=12
GET /api/manager/query/count?widgetId=12&countMode=total
GET /api/manager/query/count?widgetId=12&countMode=model&countModelId=10
```

관련 위젯 정의: [PAGE_WIDGET_API.md](../device/PAGE_WIDGET_API.md)  
DDL: [`17_page_widget_count.sql`](../../sql/schema/17_page_widget_count.sql)

---

## 위젯 생성 (count)

| 필드 | 필수 | 설명 |
|------|------|------|
| `queryKind` | ✅ | `"count"` |
| `countMode` | 권장 | `total` \| `by_model` \| `model` (미지정 시 조회 기본값 `by_model`) |
| `countModelId` | `model`일 때 ✅ | `device_model.id` |
| `deviceIds` | ❌ | 비우거나 `[]` |
| `pointNames` | ❌ | 비우거나 `[]` |

```json
{
  "pageCode": "dashboard",
  "name": "장비 수",
  "queryKind": "count",
  "countMode": "by_model",
  "deviceIds": [],
  "pointNames": []
}
```

---

## `GET /api/manager/query/count`

| 이름 | 필수 | 설명 |
|------|------|------|
| `widgetId` | ✅ | `page_widget.id` (`queryKind=count`, enabled) |
| `countMode` | ❌ | 위젯 저장값을 덮어씀 (`total` / `by_model` / `model`) |
| `countModelId` | `model`일 때 | 위젯 저장값 또는 쿼리 파라미터 |

### 동작

1. 위젯 로드 (`queryKind=count`, enabled만)
2. `devices`에서 **enabled=true** 전체 로드
3. `countMode`에 따라 응답 구성

| `countMode` | `count` | `byModel` |
|-------------|---------|-----------|
| `total` | 전체 enabled 수 | 빈 배열 |
| `by_model` | 전체 enabled 수 | 모델별 목록 |
| `model` | 해당 `countModelId`만 | 해당 모델 1행 (없으면 0 / 빈 배열) |

### 응답 예 (`by_model`)

```json
{
  "status": 200,
  "data": {
    "widgetId": 12,
    "widgetName": "장비 수",
    "pageCode": "dashboard",
    "countMode": "by_model",
    "countModelId": null,
    "count": 3,
    "byModel": [
      { "modelId": 10, "modelName": "AP8959", "manufacturer": "APC", "count": 2 },
      { "modelId": 20, "modelName": "30XA", "manufacturer": "Carrier", "count": 1 }
    ]
  }
}
```

### 오류

| 조건 | HTTP |
|------|------|
| widgetId 없음 | 400 |
| 위젯 없음 | 404 |
| queryKind ≠ count | 400 |
| 위젯 disabled | 400 |
| countMode=model 인데 countModelId 없음 | 400 |
