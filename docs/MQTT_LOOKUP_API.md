# 공통 MQTT 설정 조회 API

Sensor Data는 MariaDB에 접속하지 않고 Manager에서 설정을 조회한다.
측정 JSON은 MQTT 브로커를 통해 Sensor Data로 전달하며 이 API에는 보내지 않는다.
기존 서비스 인증(`X-Api-Key`)을 사용한다. 인증 없이 공개하지 않는다.

## URL

- `GET /api/manager/mqtt/identities/resolve?identityType=MAC&externalId=9c%3A6b%3A00%3A9b%3A4b%3Ad7`
- `GET /api/manager/mqtt/identities/bulk`
- `GET /api/manager/mqtt/mappings/models/bulk`

응답은 기존 `status`, `timestamp`, `data` 래퍼를 유지한다.
resolve의 data는 identityId, identityType, externalId, normalizedExternalId,
deviceId, deviceName, deviceModelId, deviceModelName, enabled를 제공한다.
bulk는 동일 구조의 배열이다. 식별자와 장비가 모두 활성인 경우만 반환한다.
지원 타입은 MAC, UUID, SERIAL, DEV_EUI, DEVICE_NAME이다.
식별값은 타입별로 정규화한다. 미등록·비활성 연결은 resolve에서 200/data=null,
잘못된 형식은 400으로 처리한다.

포인트 bulk는 활성 모델 포인트를 평탄한 배열로 반환한다. 기존 LoRa 메타데이터를
유지하고 componentScope, componentType, componentPath, componentIndexField,
componentUuidField를 추가한다.

- DEVICE: payloadField는 전체 JSON 기준 경로. LoRa `object.TempC_SHT`도 그대로 사용.
- COMPONENT: componentPath의 배열을 반복하여 payloadField를 각 객체 기준으로 읽는다.
  예: componentPath=gpus, payloadField=gpu_temperature, componentIndexField=gpu_index,
  componentUuidField=gpu_uuid. 값은 설정 정의이며 Manager가 payload를 파싱하지 않는다.
- 포인트가 없는 모델의 장비도 식별값 조회에 포함된다. 포인트 배열이 비어 있으면
  Sensor Data에서 임의 추출하지 않고 설정 누락으로 처리해야 한다.

## 호환성과 전환

기존 `/api/manager/lora/endpoints/resolve`, `/endpoints/bulk`, `/mappings/models/bulk`는
LoraLookupController의 컨트롤러·URL 매핑 어노테이션을 주석 처리하여 비활성화했다.
구현은 원복용으로 보존하지만 기존 URL로는 설정을 조회할 수 없다.
LoRa 등록·수정 API와 소스 관리 API는 비활성화 대상이 아니다.
기존 Sensor Data가 LoRa 조회 URL을 사용 중이라면 공통 API로 전환하기 전까지
설정 조회·캐시 갱신이 실패할 수 있으므로 배포 시 함께 전환해야 한다.
Sensor Data가 공통 API로 전환할 때 요청 파라미터를 identityType으로,
식별자 응답의 id/idType을 identityId/identityType으로 변경해야 한다.

Sensor Data 시작 시 두 bulk API로 캐시를 채우고, 캐시 미스 시 resolve를 사용한다.
식별값 변경 시 이전 캐시 항목을 제거할 수 있도록 bulk 결과는 전체 스냅샷으로 교체해야 한다.
캐시 갱신과 MQTT 파싱·Influx 저장은 별도 Sensor Data 개발 범위다.
기존 LoRa 소스/설정 refresh 알림은 이번 변경에서 URL을 바꾸지 않는다.
GPU 시드 데이터 삽입, 에이전트 JSON 변경, 페이지별 GPU 조회도 이번 변경 범위가 아니다.
