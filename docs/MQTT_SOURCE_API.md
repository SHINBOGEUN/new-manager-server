# 공통 MQTT 수집 소스 관리

## 역할

- mqtt_source: 브로커·구독 토픽·메시지 형식·활성 여부·자격증명 키.
- mqtt_source_status: Sensor Data가 보고하는 연결/수신 상태.
- device_mqtt_identity: 수신 메시지 식별값과 내부 장비 ID 연결.
- device_model_mqtt_point: 모델별 값 추출 경로 및 배율.

소스 하나가 여러 장비의 메시지를 받을 수 있다. 소스와 장비를 1:1로 연결하지 않는다.
GPU 개수에 따라 소스/포인트 행을 추가하지 않는다.

## 소스 형식

- CHIRPSTACK_LORA: 기존 Dragino/ChirpStack JSON. 기존 LoRa 처리 규칙 유지.
- AGENT: 확정된 time/deviceInfo/object/components/metadata 에이전트 JSON. GPU 외 장비도 사용 가능.

## API

| Method | URL | 목적 |
|---|---|---|
| GET | /api/manager/mqtt/sources | 관리자 화면 소스·상태 목록 |
| POST | /api/manager/mqtt/sources | 소스 등록 (ADMIN) |
| PUT | /api/manager/mqtt/sources/{id} | 소스 수정·설정 버전 증가 (ADMIN) |
| DELETE | /api/manager/mqtt/sources/{id} | 소스·상태 제거 (ADMIN) |
| POST | /api/manager/mqtt/sources/sync | 즉시 동기화 요청 (ADMIN) |
| GET | /api/manager/mqtt/sources/bulk | Sensor Data 설정 스냅샷 |
| POST | /api/manager/mqtt/sources/{id}/status | Sensor Data 상태 보고 |

설정 스냅샷에는 비활성 소스도 포함한다. Sensor Data는 삭제된 소스를 끊고,
enabled=false도 구독을 중단해야 한다. API는 기존 서비스 인증을 사용하며 공개하지 않는다.
응답 래퍼는 기존 status/timestamp/data 구조를 유지한다.

GPU 등록 요청 예시:

```json
{
  "name": "GPU 서버 수집",
  "sourceType": "AGENT",
  "brokerUrl": "tcp://192.168.0.199:1883",
  "topic": "dcim/agent/+/telemetry",
  "clientId": null,
  "credentialKey": "gpu-agent",
  "enabled": true
}
```

응답 data에는 id/name/sourceType/brokerUrl/topic/clientId/credentialKey/enabled,
configVersion/createdDt/updatedDt/status가 포함된다. 최초 configVersion=1,
수정 시 같은 ID에서 버전이 증가한다. 비밀번호는 요청·응답·DB에 저장하지 않는다.
brokerUrl에 사용자/비밀번호를 포함할 수 없다. 실제 자격증명은 Sensor Data에서 key로 해석한다.
소스별 client ID는 동일 브로커에서 서로 달라야 한다. 빈 값이면 Sensor Data가 소스 ID 기반으로 생성해야 한다.
서로 겹치는 토픽 필터로 같은 메시지를 중복 구독하지 않도록 운영자가 확인한다.

## DB 이관

기존 DB: `sql/migration/20261014_unify_mqtt_sources.sql`을 백업 후 실행한다.
lora_mqtt_source와 lora_mqtt_source_status를 mqtt_source/mqtt_source_status로 이름 변경한다.
행을 복사/삭제하지 않으며 ID·상태·버전·브로커·토픽·credential key를 보존한다.
DDL은 자동 커밋되므로 실패 시 적용된 단계를 확인하고 이어서 처리한다.

신규 DB: 구 schema/50,51 대신 schema/60_mqtt_source.sql과 schema/61_mqtt_source_status.sql을 사용한다.
DB 이관 후 Manager를 시작해야 한다. 코드만 배포하면 테이블 이름이 맞지 않는다.
실제 운영 DB에는 이 작업에서 SQL을 자동 실행하지 않았다.

## 이전 Sensor Data 호환

- GET /api/manager/lora/sources/bulk: CHIRPSTACK_LORA만 반환하며 비활성 LoRa 소스도 포함한다.
- POST /api/manager/lora/sources/{id}/status: LoRa 소스만 상태 보고 허용.
- 기존 LoRa 소스 CRUD URL은 공통 URL로 전환한다. 운영 콘솔도 공통 API를 호출한다.
- LoRa 수집 상태 요약은 AGENT 소스를 집계하지 않는다.
- 설정 변경 후 POST /api/internal/mqtt/sources/refresh로 Sensor Data에 알린다.
  해당 API가 아직 없어서 404이면 기존 /api/internal/lora/sources/refresh로만 fallback한다.
  이 fallback은 LoRa 호환용이며 AGENT 동기화 성공을 의미하지 않는다.
- 장비·포인트 설정 갱신 알림은 현재 /api/internal/lora/config/refresh를 유지한다.

Manager 완료 후 Sensor Data에서 공통 sources/bulk·상태 보고·refresh를 구현하고,
sourceType에 따라 LoRa/AGENT 핸들러를 분기해야 GPU 동적 수집이 가능하다.
기존 고정 환경설정 GPU 구독은 그 전환 시 제거하여 중복 저장을 방지한다.
