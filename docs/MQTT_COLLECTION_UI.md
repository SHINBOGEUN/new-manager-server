# MQTT 장비 식별자와 수집 상태

Manager 재시작 후 `/ops-console.html`에서 강력 새로고침한다.

## MQTT 장비 식별자

- 기존 LoRa 엔드포인트 메뉴를 공통 화면으로 확장했다.
- MQTT 지원 모델의 장비를 선택한다. LoRa는 DEV_EUI/DEVICE_NAME, 에이전트는 MAC/UUID/SERIAL이다.
- 수정 시 장비와 식별자 유형은 고정이다. 외부 식별값과 활성 여부만 변경한다.
- 같은 논리 장비의 MAC 변경은 기존 행을 수정한다. identity PK와 device_id는 유지되며 이전 Influx 데이터는 변경하지 않는다.
- 완전히 새 장비는 장비 등록 화면에서 새 ID로 등록한다.
- 삭제는 식별자 연결만 제거한다. 장비와 이전 시계열은 삭제하지 않는다.
- 동일 외부 식별값은 비활성 행을 포함하여 중복 등록할 수 없다.

API: GET/POST `/api/manager/mqtt/identities`, PUT/DELETE `/{id}`. GET은 deviceId 필터를 지원한다.
변경은 ADMIN만 허용하고 Sensor Data 설정 갱신 이벤트를 발행한다.
기존 `/api/manager/lora/endpoints` 및 Sensor Data용 `/mqtt/identities/bulk`, `/resolve`는 유지한다.

## MQTT 수집 상태

API: GET `/api/manager/mqtt/collection-status`. 기존 LoRa 상태 API는 유지한다.

- LoRa 및 에이전트 식별자를 가진 장비를 조회한다. 여러 식별자가 있어도 장비당 한 행이며 활성 식별자를 대표로 선택한다.
- 소스 요약에는 CHIRPSTACK_LORA와 AGENT를 모두 포함한다.
- 장비 상태의 소스 존재/연결 판단은 해당 메시지 형식에 한정한다. 특정 장비와 특정 브로커의 직접 연결 정보는 아니므로 같은 형식의 다른 소스 연결만으로 실제 수신을 확정할 수 없다.
- Influx 최근 7일의 `protocol=mqtt`, `_field=value`를 조회한다.
- 마지막 포인트 수는 가장 최근 저장 시각의 시계열 값 수다. 온도/전력 두 정의 × GPU 세 개가 같은 시각이면 6이다.
- 인덱스별로 마지막 값을 구분하고 UUID 교체 및 위치 태그 변경으로 같은 슬롯이 중복 집계되지 않도록 그룹을 정한다.
- `SAVED`는 저장 이력을 뜻하며 현재 정상 발행을 보장하지 않는다. 마지막 저장 시간을 확인한다. 조회 실패는 `INFLUX_UNAVAILABLE`로 표시한다.
- 상세 런타임 오류는 기존 Sensor Data의 LoRa API에서만 제공된다. GPU 오류 열이 비어 있어도 오류가 없다는 뜻은 아니다. Sensor Data 로그에서 확인한다. 장비별 지연 판정은 이번 범위가 아니다.

DB 이관이나 Sensor Data 코드 변경은 필요하지 않다.
