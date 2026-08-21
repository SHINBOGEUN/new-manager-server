# Collection Task 설계 문서

> 버전: v4  
> 작성일: 2026-08-18  
> 수정일: 2026-08-19  
> 관련 브랜치: feat/collection-task

---

## 개요

**모델 1개 = Task 1개.**  
사용자는 모델을 고르고, 그 Task 안에서 주기 그룹(1분/5분 등)을 만든 뒤 장비를 그룹에 넣습니다.

`new-manager-server`가 해당 모델의 device / endpoint / SNMP point / instance를 보고 수집 스펙을 만들고, `new-collector-service`가 장비 단위로 병렬 수집한 뒤 MQTT로 넘깁니다. `new-sensor-data-service`가 InfluxDB에 저장합니다.

SNMP 장비 전체를 스크립트 하나에서 돌리지 않습니다. 장비 1000대 / 모델 20개면 Task는 최대 20개이고, 같은 모델 안 주기가 2개면 그룹만 2개입니다.

---

## 전체 아키텍처

```
┌─────────────────────────────────────────────────────────────────┐
│                     new-manager-server (허브)                    │
│                                                                 │
│  Task = 모델 1개                                                │
│    └── Schedule Group = cron + 선택 device 목록                  │
│                                                                 │
│  스펙 생성: model snmp point + 그룹 장비의 host/port/instance     │
└──────────────────────┬──────────────────────────────────────────┘
                       │ 그룹 단위로 collector에 등록 (REST + ApiKey)
                       ▼
┌─────────────────────────────────────────────────────────────────┐
│                  new-collector-service (실행기, DB 없음)         │
│                                                                 │
│  그룹 cron 스케줄                                               │
│    → 그룹 장비 N대 병렬 SNMP (동시 수 제한, 장비별 timeout)       │
│    → 성공한 장비부터 MQTT publish                               │
└──────────────────────┬──────────────────────────────────────────┘
                       │ topic: dcim/sensor/data  (장비 1건씩)
                       ▼
┌─────────────────────────────────────────────────────────────────┐
│                 new-sensor-data-service (저장기)                 │
│  MQTT Subscribe → device 메타 조회 → InfluxDB                   │
└─────────────────────────────────────────────────────────────────┘
```

---

## 1. 핵심 규칙

### 1.1 단위

```
device_model (예: AP8959)
  └── collection_task          모델당 1개, scriptType=SNMP
        ├── collection_task_group  cron=1분, devices=[1,2,3]
        └── collection_task_group  cron=5분, devices=[4,5]
```

| 질문 | 답 |
|---|---|
| 모델 하나 = Task 하나? | 예 |
| 주기 그룹은 Task 안에서 나누나? | 예 (1분 그룹, 5분 그룹) |
| 장비는 어떻게 넣나? | 그 모델 장비 중에서 그룹에 선택 |
| 한 장비가 1분이면서 5분? | 불가. 그룹 하나에만 속함 |
| Task PK = model_id? | 불가. 같은 모델에 SNMP+Modbus Task가 생길 수 있음 |
| Task PK = UUID? | 쓰지 않음. INT AUTO_INCREMENT + `model_id` FK |

같은 모델인데 주기만 다르게 주고 싶으면 **장비를 1분 그룹 / 5분 그룹에 나눠 담으면** 됩니다. 장비 1000대 = Task 1000개가 아닙니다.

### 1.2 ID / FK

`collection_task.id`를 `model_id`로 쓰지 않는 이유: 모델은 `PROTOCOL_TYPE`을 여러 개 가질 수 있다. SNMP Task와 Modbus Task가 같은 모델에 공존한다.

| 컬럼 | 역할 |
|---|---|
| `collection_task.id` | INT AUTO_INCREMENT |
| `collection_task.model_id` | FK → `device_model` |
| `collection_task.script_type_id` | FK → `common_code` (`PROTOCOL_TYPE`) |
| UK | `(model_id, script_type_id)` — 모델+프로토콜당 Task 1개 |

### 1.3 수집 on/off

스크립트 안에 스위치는 없다. DB 플래그로 빼고, 스펙을 다시 만든다.

| 끄고 싶은 범위 | 방법 |
|---|---|
| 그 장비 SNMP만 | `device_protocol_endpoint.enabled = false` |
| 장비 전체 | `devices.enabled = false` |
| 주기 그룹 전체 | 그룹 `active = false` |
| 모델 수집 전체 | Task `active = false` |

endpoint/장비 `enabled` API는 이미 있다. 다음에 붙일 것은 **그 변경이 해당 모델 Task 스펙 재생성 + collector 재등록으로 이어지는 연동**이다.

---

## 2. new-manager-server — 데이터 모델

### 2.1 `collection_task` — 모델당 수집 설정

| 컬럼 | 타입 | 설명 |
|---|---|---|
| `id` | INT PK AUTO_INCREMENT | |
| `name` | VARCHAR(100) | 기본값 예: `{modelName} SNMP 수집` |
| `model_id` | INT NOT NULL | FK `device_model.id` ON DELETE RESTRICT (장비가 있으면 모델 삭제 자체가 409) |
| `script_type_id` | INT NOT NULL | FK `common_code.id` (`PROTOCOL_TYPE`) |
| `active` | TINYINT(1) | Task 전체 on/off |
| `created_dt` / `updated_dt` | TIMESTAMP(6) | |

제약:

- UK `(model_id, script_type_id)`
- 생성 시 해당 모델이 그 `script_type` protocol을 갖고 있는지 검증
- 1차 구현 프로토콜: SNMP (`code = snmp`)

### 2.2 `collection_task_group` — Task 안 주기 그룹

| 컬럼 | 타입 | 설명 |
|---|---|---|
| `id` | INT PK AUTO_INCREMENT | |
| `task_id` | INT NOT NULL | FK `collection_task.id` ON DELETE CASCADE |
| `name` | VARCHAR(100) | 예: `1분 그룹` |
| `cron_expression` | VARCHAR(100) | Spring cron |
| `generated_spec` | LONGTEXT NULL | 이 그룹의 수집 스펙(JSON). JS 통짜 파일 아님 |
| `collector_job_id` | VARCHAR(100) NULL | collector에 등록된 스케줄 ID |
| `active` | TINYINT(1) | 그룹 on/off |
| `created_dt` / `updated_dt` | TIMESTAMP(6) | |

제약:

- 같은 Task 안에서 cron 중복 허용 여부: **불허** (1분 그룹이 두 개일 이유 없음)
- UK `(task_id, cron_expression)`

cron 예:

| 표현식 | 의미 |
|---|---|
| `0 */1 * * * *` | 1분마다 |
| `0 */5 * * * *` | 5분마다 |
| `0 0 * * * *` | 1시간마다 |

### 2.3 `collection_task_device` — 그룹에 속한 장비

| 컬럼 | 타입 | 설명 |
|---|---|---|
| `id` | INT PK AUTO_INCREMENT | |
| `group_id` | INT NOT NULL | FK `collection_task_group.id` ON DELETE CASCADE |
| `device_id` | INT NOT NULL | FK `devices.id` ON DELETE CASCADE |
| `created_dt` | TIMESTAMP(6) | |

제약:

- UK `(group_id, device_id)`
- **한 장비는 같은 scriptType(SNMP)에서 그룹 하나에만 속함**  
  → UK `(device_id, script_type)` 는 애플리케이션에서 Task를 타고 검증.  
  구현: `device_id` 글로벌 UK가 아니라, “같은 `collection_task.script_type_id` 아래 device 1회”  
  가장 단순하게는 SNMP Task가 모델당 1개이므로, **그 모델 Task 안에서는 device_id 유일**.
- 장비의 `model_id`가 Task의 `model_id`와 같아야 함
- 장비 삭제 시 매핑 CASCADE. 그룹 스펙 재생성

### 2.4 생성 API 예시

```http
POST /api/manager/collector/tasks
```

```json
{
  "name": "AP8959 SNMP 수집",
  "modelId": 10,
  "scriptTypeId": 9,
  "active": true,
  "groups": [
    {
      "name": "1분 그룹",
      "cronExpression": "0 */1 * * * *",
      "deviceIds": [1, 2, 3]
    },
    {
      "name": "5분 그룹",
      "cronExpression": "0 */5 * * * *",
      "deviceIds": [4, 5]
    }
  ]
}
```

그룹/장비만 수정:

```http
PUT /api/manager/collector/tasks/{taskId}/groups/{groupId}
{
  "name": "1분 그룹",
  "cronExpression": "0 */1 * * * *",
  "deviceIds": [1, 2, 3, 6]
}
```

### 2.5 API 목록 (목표)

| Method | Path | 설명 |
|---|---|---|
| `POST` | `/api/manager/collector/tasks` | Task + 그룹 + 장비 생성 |
| `GET` | `/api/manager/collector/tasks` | 목록 (`modelId`, `scriptTypeId`, `active` 필터) |
| `GET` | `/api/manager/collector/tasks/{id}` | 단건 (그룹·장비 포함) |
| `PUT` | `/api/manager/collector/tasks/{id}` | Task 메타 수정 |
| `DELETE` | `/api/manager/collector/tasks/{id}` | Task 삭제 (그룹·매핑 CASCADE, collector job 제거) |
| `PATCH` | `/api/manager/collector/tasks/{id}/toggle` | Task on/off |
| `POST` | `/api/manager/collector/tasks/{id}/groups` | 주기 그룹 추가 |
| `PUT` | `/api/manager/collector/tasks/{id}/groups/{groupId}` | 그룹 cron/장비 수정 |
| `DELETE` | `/api/manager/collector/tasks/{id}/groups/{groupId}` | 그룹 삭제 |
| `PATCH` | `/api/manager/collector/tasks/{id}/groups/{groupId}/toggle` | 그룹 on/off |

현재 구현된 CRUD는 **v4 모델**(INT PK, 모델 1 Task + 주기 그룹 + 선택 장비, 그룹 JSON spec)이다. collector 재등록(B6)은 아직 없다.

---

## 3. 스펙 생성 (스크립트 대신 JSON spec)

장비 수십~수백 대를 JS `for`로 풀어 쓰지 않는다.  
그룹마다 **같은 모델 OID + 장비 목록** JSON을 만든다. 실제 SNMP/병렬/timeout은 collector Java가 한다.

### 3.1 그룹 spec 예시

```json
{
  "taskId": 1,
  "groupId": 11,
  "modelId": 10,
  "protocol": "snmp",
  "cronExpression": "0 */1 * * * *",
  "community": "public",
  "timeoutMs": 2000,
  "retries": 1,
  "maxConcurrency": 10,
  "oids": [
    { "name": "V", "template": "1.3.6.1.4.1.318.1.1.26.8.3.3.1.2.1.{instanceId}.3", "requiresInstance": true },
    { "name": "A", "template": "1.3.6.1.4.1.318.1.1.26.8.3.3.1.2.1.{instanceId}.4", "requiresInstance": true }
  ],
  "targets": [
    { "deviceId": 1, "host": "192.168.1.10", "port": 161, "instanceId": 1 },
    { "deviceId": 2, "host": "192.168.1.11", "port": 161, "instanceId": 1 }
  ]
}
```

- OID 템플릿은 모델에서 한 번만
- `{instanceId}`는 collector가 장비별로 치환
- `requiresInstance=true`인데 instance 없는 장비는 `targets`에서 제외 (skip 로그)

### 3.2 수집 대상에 넣는 조건 (그룹 장비 후보)

그룹에 담을 때와 spec 생성 때 모두 적용. 미충족은 **그 장비만 skip**, Task/그룹 생성은 실패시키지 않음.

| 조건 | 미충족 시 |
|---|---|
| 장비 `model_id` = Task `model_id` | 그룹 추가 거부 (400) |
| 이미 같은 Task의 다른 그룹에 속함 | 400 |
| `devices.enabled = true` | spec에서 제외 |
| 해당 protocol endpoint 존재 + `enabled = true` | spec에서 제외 |
| 모델에 해당 protocol 존재 | Task 생성 거부 (400) |
| point `enabled = true` | 그 point만 제외 |
| `requiresInstance = true` 이면 instance 존재 | 그 point(또는 장비) skip |
| 수집 가능한 point 1개 이상 | 장비 skip |

### 3.3 변경 시 재생성 범위

예전처럼 SNMP Task 전부를 다시 만들지 않는다. **영향 받은 모델의 그 Task/그룹만** 재생성한다.

| 이벤트 | 동작 |
|---|---|
| 그룹 deviceIds 변경 | 그 그룹 spec만 재생성 + collector 재등록 |
| 장비 추가 | 해당 모델 Task의 **기본 그룹**(첫 번째 active 그룹, 없으면 `기본 그룹` 1분 cron)에 자동 편입 후 spec 재생성. 이미 다른 그룹에 있으면 그대로 둠 |
| Task 생성 | 그룹이 없으면 기본 그룹을 만들고, 아직 안 들어간 그 모델 장비를 기본 그룹에 넣음 |
| 장비 삭제 | 매핑 제거 후 그 모델 Task spec 재생성 |
| 장비 `enabled` / 이름 / 위치 변경 | 속한 그룹 spec 재생성 |
| 장비 모델 변경 A→B | A 그룹에서 제거, B Task 기본 그룹에 편입, 양쪽 spec 재생성 |
| endpoint host/port/`enabled` | 그 장비 그룹 spec 재생성 |
| SNMP instance 변경 | 그 장비 그룹 spec 재생성 |
| 모델 SNMP point 변경 | 그 모델 Task의 **모든 그룹** spec 재생성 |
| Task/그룹 toggle | spec 재생성 없이 collector 스케줄 on/off |
| 그룹 장비 0대 | 그룹 유지, spec `targets: []`, collector는 no-op. 자동 삭제 안 함 |

훅 위치:

- `DeviceQueryService` create / update(enabled·model·이름) / delete
- `DeviceProtocolEndpointQueryService` create/update/delete
- `DeviceSnmpInstanceQueryService` create/update/delete
- `DeviceModelSnmpPointQueryService` create/update/delete
- `DeviceModelQueryService` protocol 교체
- Task/그룹 CRUD 서비스

공통 진입점: `CollectionScriptSyncService.regenerateByModelId(modelId)`  
collector 재등록은 `new-collector-service` 연동(B6) 이후.

### 3.4 검증 / 고아 방지

- spec은 스냅샷이다. JS/JSON을 부분 수정하지 않고 통째로 다시 만든다.
- 재생성 결과가 기존과 같으면 collector 호출 skip
- capabilities/현재 DB로 만든 spec vs 저장 spec 불일치 시 재동기화 (훅 누락 안전망)
- collector는 in-memory → 재시작 시 manager가 active 그룹을 다시 push

하지 않는 것:

- 장비 1대 삭제로 Task 전체 삭제
- SNMP 전체 1개 거대 스크립트
- JS `for`로 장비 순차 SNMP
- `collection_task.id = deviceId` 또는 `= modelId`

---

## 4. new-collector-service — 실행 / 병렬 / timeout

### 4.1 왜 JS 순차 루프를 쓰지 않는가

기존 collector `SnmpService`는 **timeout 5초 + retry 1회**.  
JS에서 장비 30대를 순차로 돌리면, 죽은 장비 10대가 앞에 있을 때 나머지가 약 100초 밀린다.

그룹에 장비가 여러 대여도 **한 대 실패가 다른 대를 막으면 안 된다.**

### 4.2 실행 모델

```
그룹 cron tick
  → targets를 최대 maxConcurrency(기본 10)로 병렬 SNMP
  → 장비 1대 = snmpGetMultiple 1회 (그 장비 OID 묶음)
  → 성공한 장비는 즉시 MQTT publish
  → 실패/timeout 장비는 skip + 로그, 다른 장비 계속
```

| 항목 | 규칙 |
|---|---|
| 장비 간 | **병렬** (동시 수 제한) |
| 한 장비의 OID들 | `snmpGetMultiple` 1회 (순차 GET 아님) |
| 장비별 timeout | 기본 2000ms (기존 5초보다 짧게) |
| retry | 기본 1회 |
| 응답 없음 | 그 장비만 skip. 그룹 전체 중단 없음 |
| 결과 전달 | 전체 모아 return 하지 않음. **성공 장비부터 MQTT** |
| 이전 tick이 아직 돌 때 | 새 tick skip (겹침 방지). 로그 warning |

### 4.3 MQTT payload (장비 1건)

topic: `dcim/sensor/data`

```json
{
  "datetime": "2026-08-19 02:00:00",
  "data": {
    "1": {
      "V": 220.1,
      "A": 1.4
    }
  },
  "type": "schedule"
}
```

`data` 키는 `devices.id`를 문자열로 쓴다 (예: `"9"`). 레거시 MQTT envelope와 동일하다.

실패는 MQTT에 안 넣는다. collector 로그(이후 메트릭)만.

### 4.4 collector API (목표)

| Method | Path | 설명 |
|---|---|---|
| `POST` | `/api/jobs/register` | 그룹 spec 수신, in-memory cron 등록 |
| `PUT` | `/api/jobs/{collectorJobId}` | spec 교체 (재등록) |
| `DELETE` | `/api/jobs/{collectorJobId}` | 제거 |
| `PATCH` | `/api/jobs/{collectorJobId}/toggle` | 스케줄 on/off |
| `GET` | `/api/jobs` | 현재 job 목록 |
| `GET` | `/api/health` | 헬스 |

자체 DB 없음. manager가 master.

---

## 5. new-sensor-data-service

- Spring Boot 3.x + Java 17
- MQTT `dcim/sensor/data` 구독
- `data`의 키(`devices.id` 문자열, 예 `"9"`) 기준으로 manager device 조회 (location/model/type tag용)
- InfluxDB write: **포인트 1개 = Point 1개 (narrow)**

PDU·온습도·GPU 등 모델이 달라도 measurement/tag/field 구조는 같다.  
metric 이름은 tag `point_name`, 값은 항상 field `value`(float).  
GPU처럼 장비 안 슬롯이 있으면 tag `component`만 추가한다 (없으면 생략).

**measurement:** `dcim_sensor`  
**tags (필수):** `device_id`, `point_name`, `protocol`  
**tags (manager 조회 성공 시):** `location_code`, `model_id`, `device_type`  
**tags (슬롯이 있을 때만):** `component`  
**field:** `value` (항상 float)

`location_code`는 `location_node.code` (표시명·옛 PDU path 아님. 좌/우는 location 트리).  
`protocol`은 MQTT에 없으므로 현재 `snmp` 고정.  
manager 조회 실패 시 optional tag는 생략하고 `device_id`+`point_name`으로 저장.

MQTT 1건 예시:

```json
{
  "datetime": "2026-08-20 10:51:00",
  "data": { "9": { "V": 219, "W": 519.5 } },
  "type": "schedule"
}
```

Influx line protocol (Point 2개):

```
dcim_sensor,device_id=9,device_type=PDU,location_code=RACK01,model_id=10,point_name=V,protocol=snmp value=219.0 <ms>
dcim_sensor,device_id=9,device_type=PDU,location_code=RACK01,model_id=10,point_name=W,protocol=snmp value=519.5 <ms>
```

GPU 예시:

```
dcim_sensor,device_id=40,component=0,point_name=gpu_temperature,protocol=snmp value=72.0 <ms>
```

조회:

```
from(bucket: "dcim")
  |> range(start: -1h)
  |> filter(fn: (r) => r._measurement == "dcim_sensor")
  |> filter(fn: (r) => r.device_id == "9")
  |> filter(fn: (r) => r.point_name == "V")
  |> last()
```

실패한 장비는 메시지가 오지 않으므로 이전 값이 유지된다 (gap은 조회 시 공백).

---

## 6. 인증

```
new-manager-server → new-collector-service : X-Api-Key
new-sensor-data-service → new-manager-server : X-Api-Key
```

---

## 7. 현재 코드 vs v4

| 항목 | 지금 코드 (feat/collection-task) | v4 목표 |
|---|---|---|
| Task PK | INT AUTO_INCREMENT | INT |
| 대상 | 모델 1 Task + 주기 그룹 + 선택 장비 | 모델 1 Task + 주기 그룹 + 선택 장비 |
| cron | 그룹마다 1개 | 그룹마다 1개 |
| 생성 파라미터 | name, modelId, scriptTypeId, groups[].deviceIds | modelId, scriptTypeId, groups[].deviceIds |
| 산출물 | 그룹 JSON spec | 그룹 JSON spec |
| 재생성 | 해당 모델 Task/그룹만 | 해당 모델 Task/그룹만 |
| collector | 그룹 job 등록, 병렬 SNMP, 장비별 MQTT | 그룹 job 등록, 병렬 SNMP, 장비별 MQTT |
| endpoint enabled | 해당 그룹 spec + collector 재등록 | 해당 그룹 spec + collector 재등록 |

---

## 8. 작업 순서

상태: `[x]` 완료 / `[~]` 진행·구모델로 완료(v4 수정 필요) / `[ ]` 미착수

### A. new-manager-server — 구모델 (이미 한 것)

| 순서 | 상태 | 작업 | 비고 |
|---|---|---|---|
| A1 | [x] | `V016__create_collection_task.sql` | INT PK, model 1 Task + 주기 그룹 + 장비 매핑 |
| A2 | [x] | CollectionTask 도메인/리포지토리/서비스 | UUID Task CRUD |
| A3 | [x] | `/api/manager/collector/tasks` CRUD | 생성/조회/수정/삭제/toggle |
| A4 | [x] | `CollectionScriptGenerator` JS 자동 생성 | v4에서 그룹 JSON spec으로 교체 완료 |
| A4.5 | [x] | 장비/endpoint/point/instance 변경 시 재생성 | `regenerateByModelId`. collector 재등록은 B6 |
| A4.5-on | [x] | endpoint/device `enabled` 컬럼·API | 이미 있음. 연동만 v4에 맞춤 |

### B. new-manager-server — v4 모델·그룹 (다음)

| 순서 | 상태 | 작업 | 상세 |
|---|---|---|---|
| B1 | [x] | DDL V016 | `collection_task` INT PK + `model_id`. `collection_task_group`, `collection_task_device` |
| B2 | [x] | 도메인 | Task / Group / TaskDevice 엔티티, UK, 모델·프로토콜·장비 소속 검증 |
| B3 | [x] | Task/그룹/장비 API | 섹션 2.5. 생성 시 groups[].deviceIds |
| B4 | [x] | 그룹 spec 생성기 | 모델 OID + 그룹 targets(host/port/instance). skip 규칙 3.2 |
| B5 | [x] | 동기화 훅 수정 | `regenerateByModelId`. 장비 모델 변경 시 이전/이후 모델 그룹만 |
| B6 | [x] | collector REST client | 그룹 spec register/update/delete/toggle. ApiKey. fail-fast·재시도 |

### C. new-collector-service

| 순서 | 상태 | 작업 | 상세 |
|---|---|---|---|
| C1 | [x] | Spring Boot 3.x 프로젝트 | DB 없음 |
| C2 | [x] | job 수신 API + in-memory cron | 그룹 단위 스케줄 |
| C3 | [x] | 병렬 SNMP 실행기 | maxConcurrency, timeoutMs, retries. 실패 장비 skip |
| C4 | [x] | 성공 장비 즉시 MQTT publish | payload 4.3. 전체 집계 return 없음 |
| C5 | [x] | tick 겹침 방지 | 이전 실행 중이면 skip |

### D. new-sensor-data-service

| 순서 | 상태 | 작업 | 상세 |
|---|---|---|---|
| D1 | [x] | Spring Boot 3.x 프로젝트 | 포트 8082 |
| D2 | [~] | MQTT 구독 + manager 메타 + Influx write | narrow 스키마 적용. 로컬 E2E(Influx enabled)는 다음 |

### 다음에 바로 할 일

1. **로컬 E2E** — 8080 + 8081 + 8082 + MQTT 1883 + Influx 8086, `INFLUX_ENABLED=true`  
2. **1년 보관** — raw 버킷 단기 + 1시간 다운샘플 버킷 (운영 설정)
