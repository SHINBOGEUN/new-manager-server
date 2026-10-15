# 기존 장비 화면의 MQTT 식별값 관리

장비 추가는 기존 `POST /api/manager/device-registrations`, 장비 수정은
`PUT /api/manager/devices/{id}`를 사용한다. 별도의 식별자 CRUD URL을 추가하지 않는다.
식별값은 장비·자산 정보와 같은 트랜잭션에서 저장한다.

## 장비 등록

```json
{
  "device": {
    "modelId": 8,
    "locationNodeCode": "UNASSIGNED",
    "name": "GPU 서버 1",
    "mqttIdentities": [
      { "identityType": "MAC", "externalId": "9c:6b:00:9b:4b:d7", "enabled": true }
    ]
  },
  "protocolTypeId": 10
}
```

`modelId`, `protocolTypeId`는 현장 DB의 실제 값이다. 선택한 프로토콜이 MQTT이면
IP/port endpoint와 수집 그룹을 보내지 않는다. 식별값을 생략한 MQTT 마법사 등록은 거부한다.
기존 `POST /api/manager/devices`도 같은 `mqttIdentities` 필드를 지원한다.
기본 장비 API에서는 식별값 없이 장비부터 등록하고 이후 수정으로 연결할 수 있다.

## 장비 수정 및 조회

```json
{
  "modelId": 8,
  "locationNodeCode": "UNASSIGNED",
  "name": "GPU 서버 1",
  "mqttIdentities": [
    { "identityType": "MAC", "externalId": "AA:BB:CC:DD:EE:FF", "enabled": true }
  ]
}
```

- 같은 유형의 값 변경은 기존 identity PK와 `device_id`를 유지한다.
- `mqttIdentities`는 현재 목록이다. 생략/null이면 유지, `[]`이면 연결을 제거한다.
- 목록에는 같은 유형을 중복해서 보낼 수 없다. 다른 장비가 사용하는 식별값은 비활성 상태여도 충돌로 처리한다.
- 동일 논리 장비의 MAC 변경은 기존 장비 수정으로, 별도 서버 추가는 새 장비 등록으로 처리한다.
- MQTT를 지원하는 모델에만 식별값을 연결한다. LoRa 모델은 DEV_EUI/DEVICE_NAME, 기타 MQTT 모델은 MAC/UUID/SERIAL을 사용한다.
- 기존 LoRa 등록의 최상위 `loraIdentity` 요청도 지원한다. 새 목록과 동시에 보내면 거부한다.
- 잘못된 MAC/중복 식별값 등 저장 실패 시 장비·자산·식별값 변경 전체를 롤백한다.
- `GET /api/manager/devices/{id}`와 목록 응답의 `mqttIdentities`에는 현재 ID·유형·원본·정규화 값·사용 여부를 포함한다.
- MQTT 설정 변경은 기존 LoRa 설정 갱신 연결에도 알린다. GPU 수신/저장은 후속 Sensor Data 구현 범위다.

## 화면

등록 마법사에서 MQTT 선택 시 IP/port 대신 식별자 종류와 값을 입력한다.
장비 상세의 기본 정보에서 현재 식별값을 추가·수정·제거하고 기존 저장 버튼으로 반영한다.
LoRa 전용 화면과 API도 공통 저장소를 계속 사용한다.

이번 변경에는 추가 DB 이관이 없다. 공통 MQTT 테이블 이관이 완료된 DB를 사용한다.
