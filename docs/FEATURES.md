# manager-server 기능 정리

기존 `manager-server` 프로젝트의 기능·API·인프라 구성을 정리한 문서입니다.  
`new-manager-server` 재작성 시 참고용으로 사용합니다.  
다음 개발 대기열은 [BACKLOG.md](./BACKLOG.md)를 본다. 이 문서는 구 기능 인벤토리일 뿐, API를 그대로 옮기지 않는다.

> 기준 프로젝트: `manager-server` v1.0.90  
> API prefix: `/api/manager`

---

## 1. 기술 스택

| 구분 | 기술 |
|------|------|
| 언어 / 런타임 | Java 17 |
| 프레임워크 | Spring Boot 3.5.3 |
| RDB | MariaDB (JPA/Hibernate) |
| 시계열 DB | InfluxDB 2.x |
| 인증 | JWT (JJWT), Spring Security, OAuth2 Client |
| API 문서 | SpringDoc OpenAPI (Swagger UI) |
| 모니터링 | Spring Actuator, Micrometer → Influx |
| 기타 | SNMP4J, Eclipse Paho MQTT, Apache POI, WebSocket, AOP |

---

## 2. 인프라 / 외부 연동

| 연동 대상 | 용도 |
|-----------|------|
| MariaDB | 디바이스, 자산, 사용자, 설정, Slack, 리포트 스케줄 등 메타데이터 |
| InfluxDB | 센서·전력·GPU 등 시계열 텔레메트리 조회 |
| Collector Service | 수집 스크립트·스케줄 태스크 원격 관리 (`collector.service.url`) |
| MQTT Broker | PDU SNMP 실시간 수집 결과 발행 (`pdu.mqtt.telemetry`) |
| Slack API | 알림 봇·채널 연동 |

### 프로파일

| 프로파일 | 설명 |
|----------|------|
| `test` | 개발/테스트 (기본 active) |
| `prod` | 운영 (환경변수 기반) |
| `local` | 테스트용 H2 in-memory (test resources) |

### Docker

- Base image: `eclipse-temurin:17-jre-jammy`
- Registry: `repo-docker.vivans.net/dcim/em/{artifactId}`
- Maven `docker-maven-plugin`으로 `install` 시 build/push

---

## 3. 패키지 구조 (기존)

```
net.vivans.dcim
├── ManagerServerApplication
├── aop/              # 요청 로깅 AOP
├── config/           # Security, JPA, InfluxDB, Swagger, Scheduling, RestTemplate
├── domain/           # 비즈니스 도메인 (아래 상세)
├── exception/        # 전역 예외 처리
├── filter/           # JWT, API Key 필터
├── global/           # ApiResponse, ChartData 등 공통 응답
├── property/         # InfluxDB 등 설정 프로퍼티
└── util/             # JWT, Excel, 날짜, 스크립트 템플릿 등
```

### domain 하위 모듈

| 모듈 | 설명 |
|------|------|
| `alert` | 알람 이력 조회 |
| `analysis` | 센서·전력·온습도 분석 차트 |
| `asset` | 자산(존/랙/디바이스) CRUD, Excel import |
| `auth` | 로그인, 회원가입, 토큰 갱신 |
| `common` | 공통코드 관리 |
| `cooling` | 냉각(칠러, 밸브, OA 조건) |
| `dashboard` | 대시보드 위젯 (전력, PUE, PDU, 랙 등) |
| `dashboardGroup` | 대시보드 그룹·디바이스 매핑 |
| `device` | 디바이스 계층, PDU, 수집 스크립트 |
| `entity` | JPA 엔티티 |
| `file` | 파일 업로드/다운로드 |
| `gpu` | GPU 서버 모니터링 |
| `model` | 3D 모델 메타데이터 |
| `modbus` | Modbus 제어 설정 (내부용) |
| `pduTelemetry` | PDU SNMP → MQTT 실시간 텔레메트리 |
| `pduTest` | PDU OID 테스트·배치 테스트 |
| `report` | 리포트 스케줄 CRUD |
| `setting` | 랙 색상, 전력 임계값 설정 |
| `slack` | Slack 봇·채널 설정 |
| `space` | 랙 공간·점유율 통계 |

### 주요 JPA 엔티티

`User`, `Device`, `Asset`, `AssetColor`, `Model`, `CommonCode`, `FileInfo`,  
`DashboardGroup`, `DashboardGroupDeviceMapping`, `CollectionScript`,  
`AlertHistory`, `ReportSchedule`, `PowerThresholdSetting`, `SettingRackColor`,  
`SlackBotConfig`, `SlackChannelConfig`, `PduTestOid`

---

## 4. API 기능 목록

### 4.1 인증 (`/api/manager/auth`)

| Method | Path | 기능 |
|--------|------|------|
| POST | `/login` | 로그인, JWT 발급 |
| POST | `/register` | 회원가입 |
| POST | `/refresh` | 액세스 토큰 갱신 |
| GET | `/validate` | 토큰 유효성 검증 |

### 4.2 공통코드 (`/api/manager/common-codes`)

| Method | Path | 기능 |
|--------|------|------|
| GET | `/` | 목록 조회 |
| POST | `/` | 등록 |
| PUT | `/{id}` | 수정 |

### 4.3 자산 (`/api/manager/assets`)

| Method | Path | 기능 |
|--------|------|------|
| GET | `/` | 자산 목록 (필터) |
| GET | `/{id}/zone` | 존 상세 |
| GET | `/{id}/rack` | 랙 상세 |
| GET | `/{id}/device` | 디바이스 상세 |
| POST | `/` | 자산 생성 |
| PUT | `/{name}` | 자산 수정 |
| DELETE | `/{name}` | 자산 삭제 |
| GET | `/types` | 자산 타입 목록 |
| POST | `/import` | Excel 일괄 등록 |

### 4.4 디바이스 (`/api/manager/devices`)

| Method | Path | 기능 |
|--------|------|------|
| GET | `/flat` | 플랫 목록 |
| GET | `/hierarchy` | 계층 구조 |
| GET | `/root-parents` | 루트 부모 목록 |
| GET | `/{parentDeviceId}/children` | 자식 디바이스 |
| GET | `/{deviceId}/descendants` | 하위 전체 |
| GET | `/{deviceId}` | 단건 조회 |
| POST | `/` | 디바이스 생성 |
| PUT | `/{deviceId}` | 수정 |
| DELETE | `/{deviceId}` | 삭제 |
| GET | `/pdus` | PDU 목록 |
| POST | `/import` | Excel import |
| DELETE | `/import` | Excel import 롤백 |

### 4.5 PDU 디바이스 (`/api/manager/devices/pdu`)

| Method | Path | 기능 |
|--------|------|------|
| POST | `/` | PDU 생성 |
| PUT | `/{pduDeviceId}` | PDU 수정 |
| DELETE | `/{deviceId}` | PDU 삭제 |

### 4.6 수집 스크립트 (`/api/manager/collection-scripts`)

| Method | Path | 기능 |
|--------|------|------|
| GET | `/` | 목록 |
| GET | `/{idx}` | 상세 |
| POST | `/` | 등록 |
| PUT | `/{idx}` | 수정 |
| DELETE | `/{idx}` | 삭제 |

### 4.7 GPU (`/api/manager/devices/gpus`)

| Method | Path | 기능 |
|--------|------|------|
| GET | `/` | GPU 대시보드 목록 |
| GET | `/{id}/stats` | GPU 상세 통계 |

### 4.8 3D 모델 (`/api/manager/models`)

| Method | Path | 기능 |
|--------|------|------|
| GET | `/` | 모델 목록 |
| POST | `/` | 모델 등록 |
| DELETE | `/{id}` | 모델 삭제 |

### 4.9 공간 / 랙 (`/api/manager/spaces`)

| Method | Path | 기능 |
|--------|------|------|
| GET | `/` | 공간 목록 |
| GET | `/rack/{name}` | 랙 상세 |
| GET | `/racks` | 랙 목록 |
| GET | `/racks/count` | 랙 수 |
| GET | `/racks/count/empty` | 빈 랙 수 |
| GET | `/racks/count/formfactor/total` | 폼팩터별 총 U |
| GET | `/racks/count/formfactor/remaining` | 폼팩터별 잔여 U |
| GET | `/racks/count/devices` | 디바이스 점유 랙 수 |

### 4.10 대시보드 (`/api/manager/dashboard`)

| Method | Path | 기능 |
|--------|------|------|
| GET | `/power/total` | 총 전력 |
| GET | `/power/pie` | 전력 파이 차트 |
| GET | `/pue` | PUE |
| GET | `/chiller` | 칠러 상태 |
| GET | `/cooler` | 쿨러 상태 |
| GET | `/psychrometric/chart` | 공기선도 차트 |
| GET | `/racks/space` | 랙 공간 |
| GET | `/racks/rank/power` | 랙 전력 순위 |
| GET | `/racks/rank/temp` | 랙 온도 순위 |
| GET | `/dragino/last` | Dragino 센서 최신값 |
| GET | `/pdu/realTime` | PDU 실시간 데이터 |
| GET | `/pdu/widget/stats` | PDU 위젯 통계 |
| GET | `/pdu/total-energy/chart` | PDU 총 에너지 차트 |
| GET | `/pdu/energy-ratio` | PDU 에너지 비율 |

### 4.11 대시보드 전력 (`/api/manager/dashboard/power`)

| Method | Path | 기능 |
|--------|------|------|
| GET | `/` | 전력 요약 |
| GET | `/ups` | UPS 상세 |

### 4.12 대시보드 환경 (`/api/manager/dashboard/environment`)

| Method | Path | 기능 |
|--------|------|------|
| GET | `/` | 환경 요약 |
| GET | `/dragino/last` | Dragino 최신값 |
| GET | `/pdu/temp-hum` | PDU 온습도 |
| GET | `/{id}/trend` | 존 트렌드 |
| GET | `/{id}/rack` | 랙 환경 |
| GET | `/zone/in-row` | In-Row 존 |
| GET | `/zone/rdc` | RDC 존 |
| GET | `/zone/immersion` | Immersion 존 |
| GET | `/zone/wall-coil` | Wall Coil 존 |

### 4.13 대시보드 그룹 (`/api/manager/dashboard/group`)

| Method | Path | 기능 |
|--------|------|------|
| GET | `/` | 그룹 목록 |
| GET | `/{id}` | 그룹 상세 |
| POST | `/` | 그룹 생성 |
| PUT | `/{id}` | 그룹 수정 |
| DELETE | `/{id}` | 그룹 삭제 |
| GET | `/{id}/devices` | 그룹 디바이스 |
| POST | `/devices` | 디바이스 매핑 |
| DELETE | `/{id}/devices` | 매핑 해제 |

### 4.14 냉각 (`/api/manager/cooling`)

| Method | Path | 기능 |
|--------|------|------|
| GET | `/` | 냉각 요약 |
| GET | `/valves` | 밸브 상태 |
| GET | `/{deviceId}` | 디바이스별 냉각 |
| GET | `/chiller` | 칠러 |
| GET | `/oa` | 외기(OA) 조건 |

### 4.15 분석 (`/api/manager/analysis`)

| Method | Path | 기능 |
|--------|------|------|
| GET | `/devices` | 분석 대상 디바이스 |
| GET | `/chart` | 센서 차트 |
| GET | `/sensor` | 센서 상세 |
| GET | `/cooler` | 쿨러 분석 |
| GET | `/trend/power` | 전력 트렌드 |
| GET | `/trend/temp-hum` | 온습도 트렌드 |

### 4.16 PDU 분석 (`/api/manager/pdu/analysis`)

| Method | Path | 기능 |
|--------|------|------|
| GET | `/` | PDU 전력·역률·일/월 차트·테이블 |

### 4.17 알람 (`/api/manager/alarm`)

| Method | Path | 기능 |
|--------|------|------|
| GET | `/` | 알람 이력 (필터) |

### 4.18 설정 (`/api/manager/setting`)

| Method | Path | 기능 |
|--------|------|------|
| GET | `/` | 설정 조회 (랙 색상 등) |
| POST | `/rack` | 랙 설정 저장 |
| GET | `/power-threshold` | 전력 임계값 조회 |
| POST | `/power-threshold` | 전력 임계값 등록 |
| PUT | `/power-threshold` | 전력 임계값 수정 |
| DELETE | `/power-threshold` | 전력 임계값 삭제 |

### 4.19 파일 (`/api/manager/files`)

| Method | Path | 기능 |
|--------|------|------|
| GET | `/` | 파일 목록 |
| GET | `/download/{filename}` | 다운로드 |
| POST | `/upload/{type}` | 업로드 |

### 4.20 Slack (`/api/manager/slack`)

| Method | Path | 기능 |
|--------|------|------|
| GET | `/bot-configs` | 봇 설정 목록 |
| GET | `/bot-configs/{idx}` | 봇 상세 |
| POST | `/bot-configs` | 봇 등록 |
| PUT | `/bot-configs/{idx}` | 봇 수정 |
| PUT | `/bot-configs/{idx}/channels` | 채널 동기화 |
| DELETE | `/bot-configs/{idx}` | 봇 삭제 |
| GET | `/channels` | 채널 목록 |
| GET | `/channels/{idx}` | 채널 상세 |
| POST | `/channels` | 채널 등록 |
| PUT | `/channels/{idx}` | 채널 수정 |
| POST | `/channels/{idx}/toggle` | 채널 활성 토글 |
| DELETE | `/channels/{idx}` | 채널 삭제 |

### 4.21 리포트 스케줄 (`/api/manager/report-schedules`)

| Method | Path | 기능 |
|--------|------|------|
| GET | `/` | 스케줄 목록 |
| GET | `/{id}` | 스케줄 상세 |
| POST | `/` | 스케줄 등록 |
| PUT | `/{id}` | 스케줄 수정 |
| DELETE | `/{id}` | 스케줄 삭제 |
| PATCH | `/{id}/toggle` | 활성/비활성 토글 |

### 4.22 PDU MQTT 텔레메트리 (`/api/manager/pdu-mqtt`)

| Method | Path | 기능 |
|--------|------|------|
| GET | `/selection` | 현재 SNMP 수집 대상 PDU |
| PUT | `/selection` | 수집 대상 변경 |
| DELETE | `/selection` | 수집 세션 종료 |

### 4.23 PDU 테스트 (`/api/manager/pdu-test`)

| Method | Path | 기능 |
|--------|------|------|
| GET | `/oid` | OID 목록 |
| POST | `/oid` | OID 등록 |
| PUT | `/oid/{idx}` | OID 수정 |
| DELETE | `/oid/{idx}` | OID 삭제 |
| POST | `/batch` | 배치 SNMP 테스트 |

---

## 5. 백그라운드 / 비동기 기능

| 기능 | 설명 |
|------|------|
| PDU MQTT 텔레메트리 | 선택된 PDU에 대해 SNMP 폴링 → MQTT 발행 (설정: `interval-ms`, `session-timeout-minutes`) |
| 전력 임계값 모니터링 | 15분마다 cron (`PowerThresholdMonitoringService`) — 임계 초과 시 Slack 알림 |
| 리포트 생성 | `ReportGenerationService` — 스케줄 기반 리포트 생성 |
| WebSocket | PDU 텔레메트리 선택 변경 이벤트 푸시 |

---

## 6. 보안

- JWT Bearer 인증 (`JwtAuthenticationFilter`)
- Collector Service 연동용 API Key 필터 (`ApiKeyAuthFilter`)
- `security.auth-enabled`로 인증 on/off
- Swagger UI: `/swagger-ui.html`, API Docs: `/api-docs`

---

## 7. 재작성 시 권장 순서 (참고)

1. `config` — Security, JPA, InfluxDB, Swagger
2. `global` / `exception` / `filter` — 공통 응답·예외·인증
3. `entity` + `auth` + `common`
4. `device` + `asset` + `space`
5. `dashboard` + `cooling` + `analysis`
6. `pduTelemetry` + `setting` + `alert` + `slack`
7. `report` + `file` + `gpu` + `model` + `dashboardGroup`

---

## 8. 환경변수 요약

| 변수 | 용도 |
|------|------|
| `MARIADB_HOST`, `MARIADB_PORT`, `DB_USER`, `DB_PASS` | MariaDB |
| `INFLUXDB_HOST`, `INFLUXDB_PORT`, `INFLUXDB_TOKEN`, `INFLUXDB_ORG`, `INFLUXDB_BUCKET` | InfluxDB |
| `JWT_SECRET`, `ACCESS_TOKEN_EXPIRATION`, `REFRESH_TOKEN_EXPIRATION` | JWT |
| `MQTTBROKER_HOST`, `MQTTBROKER_PORT`, `PDU_MQTT_TOPIC`, `PDU_SNMP_COMMUNITY` | PDU MQTT |
| `COLLECTOR_SERVICE_URL`, `COLLECTOR_SERVICE_API_KEY` | Collector |
| `UPLOAD_PATH`, `DOWNLOAD_PATH` | 파일 저장 |
| `LOG_LEVEL`, `LOG_FILE_NAME` | 로깅 |
