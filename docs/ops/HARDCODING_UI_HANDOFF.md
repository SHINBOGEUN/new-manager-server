# 하드코딩 개선·서비스 통합: UI 및 배포 전달 사항

기준: Manager·Collector의 `feat/hardcoding-improvements` 브랜치(2026-11-10). Manager `main`에 이미 합쳐진 PR #85의 수집·알람 설정 변경도 함께 설명한다.

## 한눈에 보기

- **Sensor Data 서비스가 없어졌다.** MQTT 수신·InfluxDB 저장은 Collector가 한다. 화면 문구와 운영 상태 화면에서 "Sensor Data"를 "Collector"로 바꾼다.
- **회원가입이 기본으로 막힌다.** 빈 DB의 첫 계정만 가입되고(관리자), 이후 계정은 관리자가 사용자 관리에서 만든다.
- **3D 모델 역할(랙·분전반·프레임)을 서버가 내려준다.** React는 `SR_*` 키 이름 대신 `sceneRole`/`modelRole`로 판단한다.
- **다른 주소에서 API·WebSocket을 쓰려면 서버 설정(CORS_ALLOWED_ORIGINS)이 필요하다.**
- 그 밖의 기존 업무 API URL·요청·응답 필드는 바꾸지 않았다(필드는 **추가만** 있음).

## UI 담당자에게

### 1. 바뀐 API (추가·동작 변경)

| API | 변경 | UI 할 일 |
|---|---|---|
| `GET /api/manager/auth/registration` | **신규**. 인증 불필요. `{ open, initialAdmin }` | 로그인 화면에서 `open=false`면 회원가입 버튼을 숨긴다. `initialAdmin=true`면 "첫 관리자 만들기"로 표시(가입 계정이 ADMIN) |
| `POST /api/manager/auth/register` | 공개 가입이 꺼져 있고 사용자가 이미 있으면 **403** | 403 메시지를 그대로 보여주고 관리자에게 요청하도록 안내. 계정 추가는 `POST /api/manager/users`(ADMIN) |
| `GET /api/manager/scene-models` | 항목에 `sceneRole` 추가: `"RACK"`, `"PANEL"`, `"FRAME"`, `null` | 모델 관리 화면에 역할 선택 추가 권장 |
| `PATCH /api/manager/scene-models/{key}/scene-role` | **신규**(ADMIN). 본문 `{ "sceneRole": "RACK" \| "PANEL" \| "FRAME" \| null }` | 업로드한 GLB를 랙/분전반/프레임으로 쓰려면 이 값을 지정해야 한다 |
| `GET /api/manager/device-pages/{pageCode}/display` | `items[].modelRole` 추가 | 아래 2번 참고 |
| `PUT /api/manager/device-pages/{pageCode}/display` | 모델 키는 **기본 도형** 또는 **모델 카탈로그(scene_model)에 등록된 키**만 허용 | 카탈로그에 없는 키를 보내면 400 |
| `GET /api/manager/operations/collection/health` | `sensor-data` 항목 **삭제**. `collector` 항목 `details`에 `ingestEnabled`, `ingestConnected` 추가, 메시지가 수신 상태를 포함 | 상태 카드에서 Sensor Data 칸을 없애고 Collector 칸에 "MQTT 수신" 상태 표시 |

### 2. 3D 도면: 키 이름 대신 역할 사용

서버는 더 이상 `SR_.*RACK`, `SR_INROW_PANEL`, `SR_INROW_MOLD`/`SR_WALL_COIL` 같은 이름 규칙으로 판단하지 않는다. React에서도 같은 규칙을 쓰면 업로드 모델(`USR_*`)이 랙·분전반으로 동작하지 않는다.

| 역할 | 서버가 허용하는 동작 | 기본 지정 모델 |
|---|---|---|
| `RACK` | 위치(LOCATION_TYPE=RACK) 연결, 랙 색상, 랙 내부 장비 | SR_RACK, SR_DOUBLE_RACK, SR_DLC_RACK, SR_INROW_RACK, SR_OCP_RACK, SR_RDC_RACK, SR_ORV3_RACK, SR_HAC_RACK |
| `PANEL` | 분전반 구성품 배치, 분전반 사진·배치 저장 | SR_INROW_PANEL |
| `FRAME` | 높이·깊이를 폭과 별개로 조정 | SR_INROW_MOLD, SR_WALL_COIL |

- 기본 도형(RACK, PDU, CRAC, WALL*, DOOR*, WINDOW*, FLOOR 등)은 지금처럼 화면이 직접 그린다.
- 기존 HTML(`page-scene-3d.js`, `showroom-models.js` 등)은 GLB 파일 매핑 때문에 `SR_*` 키를 그대로 쓰며 이번에 바꾸지 않았다. React 이관 때 판단 로직은 역할로, 파일 매핑은 모델 목록 API의 `url`로 바꾸면 된다.

### 3. 화면 문구

- MQTT 수집 소스·상태 화면의 "Sensor Data"는 "Collector"로 바꿨다(기존 HTML 반영 완료). 예: client id 빈칸 안내, credential key 안내, 즉시 동기화 토스트, GPU 오류 확인 위치.
- 운영 상태 설명: "Manager · MariaDB · InfluxDB · Collector(수집·MQTT 수신) · MQTT".

### 4. CORS·WebSocket

- 브라우저에서 다른 주소(예: React 개발 서버 `http://dj.vivans.net:20174`)로 Manager API와 `/ws/alarms`, `/ws/mqtt-monitor`를 호출하려면 서버의 `CORS_ALLOWED_ORIGINS`에 그 주소가 들어 있어야 한다. 기본값은 `*`이지만 운영에서는 실제 화면 주소로 좁힐 예정이므로, 사용하는 화면 주소를 배포 담당자에게 알려 준다.
- WebSocket 인증은 지금처럼 1회용 ticket(`/api/manager/alarms/socket-ticket`, MQTT 모니터 세션 ticket)으로 한다. 변경 없음.
- 쿠키(refresh token)는 교차 출처로 보내지 않는다(`allowCredentials=false`). React는 같은 출처 프록시로 `/api/manager/auth/refresh`를 호출해야 한다.

### 5. 랙 전력 위젯

- 전력 균형·전력 추이·상별 위젯 대상 장비 유형은 서버 설정 `RACK_POWER_DEVICE_TYPES`(기본 `PDU`), 이중화 경로는 `RACK_REDUNDANT_PATHS`(기본 `A,B`)로 정한다. 오류 문구도 이 경로 코드로 만들어진다.
- 기존 HTML은 장비 유형을 `PDU` 문자열로 거르고 있다(`ops-console.html`). React는 서버 기본 위젯·검증 결과를 그대로 쓰고 같은 문자열 비교를 새로 만들지 않는다.

### 6. 기존 전달 내용(PR #85, 유지)

- 수집 상태 조회는 대상 장비 전체를 `devices`와 `summary`에 포함한다. 대수가 많으면 가상 스크롤 또는 페이지 처리를 검토한다.
- 온습도·설비 기본 위젯 수는 설정(`WIDGET_ENVIRONMENTAL_SOURCES` 1~8, `EQUIPMENT_VALUE_WIDGETS` 0~20)을 따른다. 저장된 위젯은 다시 만들지 않는다.
- MQTT·LoRa 최근값, 전력 분배·공기선도·랙 위젯의 최신값 조회 기간은 배포 설정으로 조정된다. 기본값은 종전과 같다.
- 기존 HTML 갱신 주기: 대시보드 15초, 랙 위젯 30초, 도어 15초, 전체 현황·알람 60초. React는 UI에서 관리한다.
- Chart.js 4.4.7, MQTT.js 5.10.4, Pretendard 1.3.9를 서버에서 제공한다(`/vendor/chartjs/**`, `/vendor/mqtt/**`, `/vendor/pretendard/**`).
- 서비스 간 키는 UI 로그인 토큰과 별개다. 화면은 항상 사용자 JWT를 쓴다.

## 배포 담당자에게

상세 절차는 [DEPLOYMENT.md](./DEPLOYMENT.md)의 **Sensor Data → Collector 통합 전환**을 따른다.

- **서비스 키는 2개로 줄었다.** `COLLECTOR_API_KEY`(Manager → Collector), `MANAGER_API_KEY`(Collector → Manager). 두 키는 서로 다른 강한 무작위 값이며 각각 양쪽 서비스에 같은 값을 넣는다. `SENSOR_DATA_API_KEY`는 더 이상 쓰지 않는다.
- **Collector에 새로 필요한 값**: `INFLUX_URL`, `INFLUX_TOKEN`(+ ORG/BUCKET/MEASUREMENT), `MANAGER_SERVICE_URL`, `MANAGER_API_KEY`, `INGEST_MQTT_CLIENT_ID`(발행용 `MQTT_CLIENT_ID`와 달라야 함). LoRa 자격증명 변수 접두사는 `SENSOR_MQTT_LORA_CREDENTIALS_` → `INGEST_MQTT_LORA_CREDENTIALS_`.
- **Manager에서 빠진 값**: `SENSOR_DATA_SERVICE_URL`, `SENSOR_DATA_API_KEY`, `SENSOR_DATA_HEALTH_ENABLED`.
- **기동 시 필수값 검사**: 값이 비어 있으면 누락된 환경변수 목록을 한 번에 출력하고 멈춘다. Manager는 `SPRING_PROFILES_ACTIVE`(test/prod)를 반드시 지정한다.
- **DB 마이그레이션**: `sql/migration/20261110_scene_model_role.sql`(scene_model.scene_role 추가, 기존 SR 모델 역할 채움), `20261110_widget_trend_defaults.sql`(재실행 안전하게 수정). 둘 다 Actions 배포의 Manager 마이그레이션 목록에 포함했다.
- **보안 기본값**: `PUBLIC_REGISTRATION_ENABLED=false`, prod `SWAGGER_ENABLED=false`, `CORS_ALLOWED_ORIGINS`는 실제 화면 주소로 제한 권장.
- 예전 소스와 Git 이력에 포함된 JWT 서명 키·Influx 토큰·DB 자격증명을 실제로 쓰고 있다면 교체한다. JWT 키를 바꾸면 기존 토큰은 무효가 된다.
- 수집·알람·위젯 기본값(SNMP_COMMUNITY, COLLECTION_*, LIVE_*, ALARM_MAX_SAMPLE_AGE, *_LOOKBACK, 위젯 수)은 PR #85와 같다. 전체 예시는 `.env.example`.
