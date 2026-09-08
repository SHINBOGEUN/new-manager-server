# PUE Query API

PUE는 위젯 preset이 아닌 독립 API로 계산합니다.

## PUE 위젯 저장

`POST /api/manager/widgets/pue`에 `pageCode`, `name`, `rangePreset`, `freshnessMinutes`, `totalSources`, `coolerSources`를 보내면 설정이 `page_widget`, `page_widget_pue`, `page_widget_pue_source`에 저장됩니다. `freshnessMinutes`는 마지막 측정값의 허용 경과 시간이며 기본값은 15분입니다. 저장된 위젯은 다음 API로 다시 선택하지 않고 조회합니다.

```http
GET /api/manager/query/pue?widgetId=30
```

수정은 `PUT /api/manager/widgets/{widgetId}/pue`로 합니다. 생성 요청에서 `pageCode`만 제외한 같은 구조를 사용합니다. 공통 `PATCH /api/manager/widgets/{widgetId}/enabled`, `DELETE /api/manager/widgets/{widgetId}`로 표시 ON/OFF와 삭제를 처리하며, 삭제 시 PUE 설정과 소스도 함께 삭제됩니다.

아래 POST 계산 API는 저장 전 미리보기에 사용합니다.

```http
POST /api/manager/query/pue
Content-Type: application/json
```

```json
{
  "totalSources": [
    { "deviceId": 1, "pointName": "TOTAL_WT" },
    { "deviceId": 2, "pointName": "T_WT" }
  ],
  "coolerSources": [
    { "deviceId": 10, "pointName": "POWER" }
  ],
  "rangePreset": "last_24h"
}
```

각 장비마다 그 장비를 대표하는 POWER 포인트 하나를 지정하므로 모델별 포인트명이 달라도 됩니다. 현재는 활성 SNMP 포인트 카탈로그를 기준으로 `DATA_POINT_TYPE=POWER`인지 검증합니다. 모든 포인트의 단위가 같아야 하며 한 장비를 두 역할에 동시에 넣을 수 없습니다. 전체 소스는 최대 200개입니다.

계산식은 `sum(totalSources 최신값) / sum(coolerSources 최신값)`입니다. 조회 기간 안에서 각 포인트의 마지막 값을 사용합니다.

```json
{
  "status": 200,
  "data": {
    "value": 3.0000,
    "totalPower": 150.00,
    "coolerPower": 50.00,
    "unit": "W",
    "rangePreset": "last_24h",
    "start": "2026-09-06T00:00:00Z",
    "end": "2026-09-07T00:00:00Z",
    "complete": true,
    "calculationStatus": "OK",
    "missingDeviceIds": [],
    "devices": [
      { "deviceId": 1, "deviceName": "Main PDU", "role": "total", "pointName": "TOTAL_WT", "value": 100.00, "unit": "W", "time": "2026-09-07T00:00:00Z" },
      { "deviceId": 10, "deviceName": "Cooler 1", "role": "cooler", "pointName": "POWER", "value": 50.00, "unit": "W", "time": "2026-09-07T00:00:00Z" }
    ]
  }
}
```

`calculationStatus`는 `OK`, `MISSING_DATA`, `ZERO_COOLER_POWER`입니다. 요청한 포인트 중 하나라도 데이터가 없거나 `freshnessMinutes`보다 오래되면 부분합으로 PUE를 만들지 않고 `value=null`, `complete=false`로 응답합니다. 오래된 장비는 `staleDeviceIds`에도 표시됩니다. Cooler 합계가 0이면 `value=null`, `complete=true`, `ZERO_COOLER_POWER`입니다.

Ops Console의 화면 위젯 탭 아래 PUE 계산 영역에서 장비와 POWER 포인트를 선택해 미리 계산하거나 저장·수정할 수 있습니다. 위젯 보드는 PUE, 총전력, Cooler 전력, 오류 사유와 장비별 상세값을 표시합니다.

기존 DB에는 [20260908_page_widget_pue_freshness.sql](../../sql/upgrade/20260908_page_widget_pue_freshness.sql)을 적용합니다.
