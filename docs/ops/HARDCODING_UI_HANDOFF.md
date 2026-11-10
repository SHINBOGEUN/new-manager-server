# 하드코딩 개선: UI 및 배포 전달 사항

기준: Manager, Collector, Sensor Data의 `feat/hardcoding-improvements` 브랜치. Manager의 `main`에 이미 합쳐진 PR #85의 수집·알람 설정 변경을 포함해 설명한다.

## UI 담당자에게

- 기존 업무 API의 URL, 요청 파라미터 및 JSON 응답 필드는 변경하지 않았다.
- 수집 상태 조회는 이전에 처음 200대까지만 포함했으나, 이제 모든 대상 장비가 `devices`와 `summary`에 포함된다. 대수가 많을 경우 목록이 길어지고 조회 시간이 늘 수 있으므로 UI 가상 스크롤 또는 페이지 처리 여부를 확인한다. 서버가 내려주는 전체 요약과 클라이언트가 현재 표시 중인 행의 수를 혼동하지 않는다.
- 온습도·설비의 기본 위젯은 설정된 소스 개수에 따라 초기 표시 대상이 달라질 수 있다. 기존에 저장된 설비 위젯 구성은 자동으로 다시 생성하지 않는다. 한 위젯의 소스 8개, 자동 값 위젯 20개 상한은 기존 검증과 UI 호환성을 위해 유지한다.
- MQTT·LoRa 최근값, 전력 분배·공기선도·랙 위젯의 최신값 조회 기간을 배포 설정으로 조정할 수 있다. 기본값은 종전과 같다. 기간을 변경하면 값 없음 표시가 달라질 수 있다.
- 기존 HTML의 갱신 주기는 대시보드 15초, 랙 위젯 30초, 도어 15초, 전체 현황·알람 60초다. React 이관 시 갱신 주기와 설정은 UI에서 관리한다. 서버에서 UI 갱신 설정 API나 `/ui-config.js`를 제공하지 않는다.
- Chart.js 4.4.7, MQTT.js 5.10.4 및 Pretendard 1.3.9를 서버에서 제공한다. 인터넷 CDN을 참조하지 않는다. 프록시에서 `/vendor/chartjs/**`, `/vendor/mqtt/**`, `/vendor/pretendard/**`를 차단하지 않아야 한다.
- 서비스 간 키는 UI 로그인 토큰과 별개다. Sensor Data의 Manager 서비스 키는 MQTT 설정 조회, 소스 상태 보고, 장비 단건 조회에만 허용된다. 임의의 관리 API를 서비스 키로 호출하던 클라이언트가 있다면 사용자 JWT 및 역할 권한을 사용해야 한다.
- 이 변경에는 기존 UI API 필드 교체, 화면 재설계 또는 DB 마이그레이션이 필요하지 않다.

## 배포 담당자에게

- `SPRING_PROFILES_ACTIVE`를 명시한다. Manager와 Sensor Data는 더 이상 기본적으로 `test` 프로필을 활성화하지 않는다. Manager `test` 프로필을 쓰는 경우에도 실제 DB와 InfluxDB 주소 및 자격증명을 환경에서 지정해야 한다.
- `JWT_SECRET`, `COLLECTOR_API_KEY`, `SENSOR_DATA_API_KEY`, `MANAGER_API_KEY`를 환경변수로 지정한다. Manager와 Sensor Data의 `MANAGER_API_KEY`는 같은 값, Manager와 Collector의 `COLLECTOR_API_KEY`는 같은 값, Manager와 Sensor Data의 `SENSOR_DATA_API_KEY`는 같은 값이어야 한다. 세 API 키는 서로 다른 강한 무작위 값으로 생성한다. 외부 데이터 API를 쓰면 `EXTERNAL_DATA_API_KEY`도 지정한다.
- 예전 소스와 Git 이력에 포함된 JWT 서명 키·Influx 토큰·DB 자격증명의 실제 사용 여부를 확인하고 사용 중이면 교체한다. 이 변경은 기존에 발급된 자격증명을 서버에서 자동 폐기하지 않는다. JWT 키를 바꾸면 해당 키로 발급된 기존 토큰은 무효가 된다.
- 서비스들이 같은 시점에 새 키를 사용하도록 배포 순서를 조율한다. 예전 고정 서비스 키는 더 이상 허용되지 않는다.
- 수집 기본값: `SNMP_COMMUNITY=public`, `COLLECTION_TIMEOUT_MS=2000`, `COLLECTION_RETRIES=1`, `COLLECTION_MAX_CONCURRENCY=10`; 실시간은 `LIVE_INTERVAL_MS=1000`, `LIVE_TIMEOUT_MS=800`, `LIVE_RETRIES=0`, `LIVE_MAX_CONCURRENCY=10`. SNMP 커뮤니티는 현재 전체 장비 공통이며 장비별 설정 기능은 추가하지 않았다.
- 알람 최신 측정 허용시간: `ALARM_MAX_SAMPLE_AGE=PT5M`. 수집 주기가 긴 장비에 맞게 ISO-8601 기간으로 조정할 수 있다.
- 최신 조회 기간: `COLLECTION_LATEST_LOOKBACK_HOURS=168`, `WIDGET_LATEST_LOOKBACK=PT24H`. 이 설정은 기존 모든 분석 API의 기간 제한을 변경하지 않는다.
- 기본 위젯 수: `WIDGET_ENVIRONMENTAL_SOURCES=8`(1~8), `EQUIPMENT_VALUE_WIDGETS=20`(0~20). 한도를 벗어난 설정은 허용 범위로 보정한다.

전체 예시는 `.env.example`을 참조한다. PR #85에 이미 포함된 변경과 이번 브랜치의 변경은 각 저장소 이력을 함께 확인한다.
