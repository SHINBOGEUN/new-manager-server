# Collection Task 설계 문서

> 버전: v3  
> 작성일: 2026-08-18  
> 관련 브랜치: feat/collection-task

---

## 개요

사용자가 수집 주기와 프로토콜만 설정하면, `new-manager-server`가 등록된 장비 메타데이터와 SNMP 관련 테이블을 조회하여 JS 스크립트를 자동 생성하고, `new-collector-service`에 전달하여 모든 장비를 주기적으로 수집하는 시스템입니다.

---

## 전체 아키텍처

```
┌─────────────────────────────────────────────────────────────────┐
│                     new-manager-server (허브)                    │
│                                                                 │
│  Task CRUD API  ──►  collection_task 테이블                     │
│       │                                                         │
│       ▼                                                         │
│  JS 스크립트 자동 생성                                            │
│  (device_protocol_endpoint + device_model_snmp_point 기반)      │
│       │                                                         │
│       ▼                                                         │
│  GET /devices/capabilities                                      │
└──────────────────────┬──────────────────────────────────────────┘
                       │ task + JS script 전달 (REST + ApiKey)
                       ▼
┌─────────────────────────────────────────────────────────────────┐
│                  new-collector-service (실행기)                  │
│                     자체 DB 없음                                  │
│                                                                 │
│  POST /api/tasks/register  ──►  in-memory Cron Scheduler        │
│                                        │                        │
│                                        ▼                        │
│                                   GraalJS 실행                   │
│                                        │                        │
│                                        ▼                        │
│                                   MQTT Publish                  │
└──────────────────────┬──────────────────────────────────────────┘
                       │ topic: dcim/sensor/data
                       ▼
┌─────────────────────────────────────────────────────────────────┐
│                 new-sensor-data-service (저장기)                 │
│                                                                 │
│  MQTT Subscribe  ──►  device 메타 조회 (캐시)  ──►  InfluxDB    │
│                        (new-manager-server API)                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 1. new-manager-server — Task CRUD

### 새로운 도메인 패키지

```
net.vivans.dcim.module.collectortask
├── domain/
│   ├── model/CollectionTask.java
│   └── repository/CollectionTaskRepository.java
├── application/
│   ├── CollectionTaskService.java
│   └── CollectionScriptGenerator.java      ← JS 자동 생성 핵심
├── infrastructure/
│   └── persistence/CollectionTaskJpaRepository.java
└── api/
    ├── CollectionTaskController.java
    └── dto/
        ├── CollectionTaskCreateRequest.java
        ├── CollectionTaskUpdateRequest.java
        └── CollectionTaskResponse.java
```

### DB 테이블: `collection_task`

| 컬럼 | 타입 | 설명 |
|---|---|---|
| `id` | UUID | PK |
| `name` | VARCHAR(100) | task 이름 |
| `cron_expression` | VARCHAR(100) | 수집 주기 (cron 형식) |
| `protocol_type_id` | INT | `common_code.id` (PROTOCOL_TYPE) |
| `generated_script` | TEXT | 자동 생성된 JS 스크립트 |
| `collector_task_id` | VARCHAR(100) | new-collector-service 등록 ID |
| `active` | BOOLEAN | 활성 여부 |
| `created_at` | TIMESTAMP | |
| `updated_at` | TIMESTAMP | |

### API 목록

| Method | Path | 설명 |
|---|---|---|
| `POST` | `/api/manager/collector/tasks` | task 생성 (전체 장비 기준 스크립트 자동 생성 + collector 등록) |
| `GET` | `/api/manager/collector/tasks` | 목록 조회 |
| `GET` | `/api/manager/collector/tasks/{id}` | 단건 조회 |
| `PUT` | `/api/manager/collector/tasks/{id}` | 수정 (스크립트 재생성 + collector 동기화) |
| `DELETE` | `/api/manager/collector/tasks/{id}` | 삭제 (collector에서도 제거) |
| `PATCH` | `/api/manager/collector/tasks/{id}/toggle` | 활성/비활성 전환 |

### Task 생성 Request 예시

```json
POST /api/manager/collector/tasks
{
  "name": "서버실 SNMP 수집",
  "cronExpression": "0 */1 * * * *",
  "scriptTypeId": 1
}
```

- `scriptTypeId`는 `common_code.id`를 사용하며, 현재는 `PROTOCOL_TYPE` 그룹의 코드(`snmp`, `modbus` 등)를 참조합니다.
- `page_code_id`는 현재 1차 구현 범위에서는 사용하지 않습니다. 지금 task는 전체 장비 대상이며, 페이지 조건은 이후 필터 기능으로 분리하는 것이 더 자연스럽습니다.

### cron_expression 예시

| 표현식 | 의미 |
|---|---|
| `0 */1 * * * *` | 1분마다 |
| `0 */5 * * * *` | 5분마다 |
| `0 0 * * * *` | 1시간마다 |
| `0 0 0 * * *` | 매일 자정 |

### Task 생성 내부 흐름

```
1. POST /api/manager/collector/tasks 요청 수신
2. collection_task 테이블에 저장
3. GET /devices/capabilities (전체 장비 기준으로 장비 목록 + SNMP OID 조회)
4. CollectionScriptGenerator → JS 스크립트 자동 생성
   - device_protocol_endpoint 에서 host, port 추출
   - device_model_snmp_point + device_snmp_instance 에서 OID 목록 resolve
5. generated_script 컬럼에 저장
6. new-collector-service POST /api/tasks/register 호출
7. 응답받은 collector_task_id를 collection_task에 저장
```

### 자동 생성 JS 스크립트 예시

> 사용자가 직접 작성하는 것이 아니라, 서버가 SNMP 테이블을 보고 자동으로 생성합니다.

```javascript
// [자동 생성] 서버실 SNMP 수집 - 2026-08-18
const result = { type: "sensor", datetime: now(), data: {} };

result.data["device:42"] = snmpGetMultiple("192.168.1.10", 161, [
  "1.3.6.1.4.1.9.9.48.1.1.1.6.1",
  "1.3.6.1.4.1.9.9.48.1.1.1.6.2",
  "1.3.6.1.2.1.25.2.3.1.6.1"
]);

result.data["device:55"] = snmpGetMultiple("192.168.1.20", 161, [
  "1.3.6.1.4.1.9.9.48.1.1.1.6.1"
]);

publish("dcim/sensor/data", JSON.stringify(result));
```

---

## 2. new-collector-service (신규 프로젝트)

### 핵심 원칙

- **자체 DB 없음** — 기존 `collector-service`의 7개 CRUD 컨트롤러 + DB 전부 제거
- **new-manager-server가 master** — task + JS 스크립트를 REST로 수신
- task는 **in-memory**로 관리
- Spring Boot 3.x + Java 17 + GraalJS

### 기존 collector-service와의 차이

| 항목 | 기존 | new |
|---|---|---|
| 데이터 저장 | 자체 DB (ScheduledTask, Device 등) | 없음 (in-memory) |
| CRUD API | 7개 컨트롤러 | 없음 |
| 장비 메타 관리 | 자체 중복 관리 | new-manager-server 의존 |
| 스크립트 입력 | 사용자 직접 작성 | new-manager-server가 자동 생성하여 전달 |
| 기술 스택 | Spring Boot 2.x | Spring Boot 3.x |

### API

| Method | Path | 설명 |
|---|---|---|
| `POST` | `/api/tasks/register` | task + JS 스크립트 수신, 스케줄러 등록 |
| `DELETE` | `/api/tasks/{id}` | task 제거 |
| `PATCH` | `/api/tasks/{id}/toggle` | 활성/비활성 |
| `GET` | `/api/tasks` | 현재 등록된 task 목록 (상태 확인) |
| `GET` | `/api/health` | 헬스 체크 |

### MQTT Payload 표준

```json
{
  "type": "sensor",
  "datetime": "2026-08-18 09:00:00",
  "data": {
    "device:42": {
      "cpu_usage": 85.3,
      "mem_usage": 70.1
    },
    "device:55": {
      "cpu_usage": 60.0
    }
  }
}
```

- `data` key: `device:{new-manager-server의 device.id}` (정수 기준)
- MQTT topic: `dcim/sensor/data`

---

## 3. new-sensor-data-service (신규 프로젝트)

### 기술 스택

- Spring Boot 3.x + Java 17
- Eclipse Paho MQTT Client
- InfluxDB Client (influxdb-client-java)
- new-manager-server REST API 호출로 device 메타데이터 조회

### 핵심 처리 흐름

```
1. MQTT dcim/sensor/data 토픽 구독
2. payload 파싱 → data key에서 device:{id} 추출
3. GET /devices/capabilities?deviceIds=42,55,... 호출 (로컬 캐시 우선)
4. InfluxDB Point 생성
5. InfluxDB write
```

### InfluxDB 태그/필드 표준

- **measurement**: `dcim_sensor`
- **tags**:
  - `device_id` — 정수 ID
  - `model_id`
  - `location_code`
  - `point_name`
  - `protocol` (SNMP 등)
- **fields**: `value` (수집된 수치값)

### 기존 sensor-data-service와의 차이

| 항목 | 기존 | new |
|---|---|---|
| device 조회 | 자체 legacy DB | new-manager-server API |
| device key 방식 | 문자열 deviceId | `device:{integer id}` |
| InfluxDB 태그 | 비정형 | 표준화 (model_id, location_code 등) |
| 기술 스택 | Spring Boot 2.x | Spring Boot 3.x |

---

## 4. 인증

서비스 간 REST 통신은 `X-Api-Key` 헤더를 사용합니다.

```
new-manager-server → new-collector-service : X-Api-Key: {collector_api_key}
new-sensor-data-service → new-manager-server : X-Api-Key: {manager_api_key}
```

---

## 5. 작업 순서

| 순서 | 서비스 | 작업 |
|---|---|---|
| 1 | new-manager-server | `collection_task` 테이블 Flyway DDL (V016__) |
| 2 | new-manager-server | CollectionTask 도메인/리포지토리/서비스 구현 |
| 3 | new-manager-server | `/api/collector/tasks` CRUD API 구현 |
| 4 | new-manager-server | `CollectionScriptGenerator` JS 자동 생성 로직 구현 |
| 5 | new-manager-server | new-collector-service REST client 구현 |
| 6 | new-collector-service | Spring Boot 3.x 신규 프로젝트 생성 (DB 없음) |
| 7 | new-collector-service | task 수신 API + in-memory 스케줄러 + GraalJS + MQTT publish |
| 8 | new-sensor-data-service | Spring Boot 3.x 신규 프로젝트 생성 |
| 9 | new-sensor-data-service | MQTT 구독 + device 메타 조회 + InfluxDB write |
