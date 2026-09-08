# PUE 정의·조회 API

PUE는 장비가 아닌 **계속 수집되는 파생 지표**다. `pue_definition`이 total/cooler source와 수집 주기를 보관하고, 위젯은 정의를 참조해 저장된 시계열만 표시한다.

## 흐름

`Manager PUE 정의 → Collector 동기화 → source 동시 수집·계산 → dcim/derived/pue MQTT → Sensor Data → dcim_sensor(metric_kind=pue)`

위젯 OFF·삭제는 수집을 멈추지 않는다. 수집 중지는 `PATCH /api/manager/pue-definitions/{id}/collection-enabled?enabled=false`만 사용한다.

Collector의 job은 메모리 기반이지만, Manager는 Collector의 기동 인스턴스 ID를 1분마다 확인한다. Manager 또는 Collector가 먼저 기동되었는지와 무관하게 ID가 새로 감지되면 활성 일반 수집 그룹과 `collection_enabled=true` PUE 정의를 모두 재등록한다.

## PUE 정의

`POST /api/manager/pue-definitions`, `PUT /api/manager/pue-definitions/{id}`, `GET /api/manager/pue-definitions`으로 totalSources/coolerSources와 `calculationCron`을 관리한다.

## PUE 위젯 저장

`POST /api/manager/widgets`에 `queryKind:"pue"`, `pueDefinitionId`, `pageCode`, `name`을 보내면 위젯이 정의를 참조한다. `pueFreshnessMinutes`는 표시 데이터의 허용 경과 시간이며 기본값은 15분이다.

```http
GET /api/manager/query/pue?widgetId=30
```

위 GET API는 원본 장비 포인트를 다시 계산하지 않는다. Sensor Data가 기존 `dcim_sensor` measurement에 저장한 `metric_kind=pue`, `pue_definition_id={id}` 레코드의 `value`, `total_power`, `cooler_power` 최신 시점을 조회해 표시한다. 저장값이 없거나 `pueFreshnessMinutes`보다 오래되면 `value=null`, `complete=false`로 응답한다.

수정은 공통 `PUT /api/manager/widgets/{widgetId}`로 `pueDefinitionId`, `pueRangePreset`, `pueFreshnessMinutes`를 보냅니다. `PATCH /api/manager/widgets/{widgetId}/enabled`, `DELETE /api/manager/widgets/{widgetId}`는 표시만 바꾸며 PUE 수집 정의와 소스에는 영향을 주지 않습니다. 수집 정의는 참조하는 위젯이 없을 때만 삭제할 수 있습니다.

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

POST 미리보기의 `calculationStatus`는 `OK`, `MISSING_DATA`, `ZERO_COOLER_POWER`입니다. 요청한 포인트 중 하나라도 데이터가 없거나 조회 기간 밖이면 부분합으로 PUE를 만들지 않고 `value=null`, `complete=false`로 응답합니다. GET 저장값 조회에서는 `OK`, `MISSING_DATA`, `STALE_DATA`를 사용하며, 오래된 저장값이면 `staleDeviceIds`에 해당 정의의 소스 장비가 표시됩니다.

Ops Console의 화면 위젯 탭 아래 PUE 수집 정의 영역에서 장비와 POWER 포인트, cron, 수집 ON/OFF를 관리합니다. 위젯 생성 폼에서는 PUE 정의를 선택합니다. 위젯 보드는 InfluxDB에 저장된 최신 PUE, 총전력, Cooler 전력과 저장 상태를 표시합니다. 장비별 원본 구성은 PUE 수집 정의에서 확인합니다.
