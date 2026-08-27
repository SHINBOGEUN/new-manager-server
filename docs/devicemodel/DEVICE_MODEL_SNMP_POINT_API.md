# DeviceModel SNMP Point API 설계

`devicemodel` 모듈의 **모델별 SNMP 수집 point**(`device_model_snmp_point`) API·비즈니스 규칙을 정리한 문서입니다.

> API prefix: `/api/manager/device-models/{modelId}/protocols/{protocolId}/snmp-points`  
> 부모 API: [DEVICE_MODEL_API.md](DEVICE_MODEL_API.md)  
> 관련 ERD: [ERD.md — device_model_snmp_point](../ERD.md#device_model_snmp_point--모델별-snmp-수집-point-devicemodel-모듈)  
> DDL: [V006__create_device_model_snmp_point.sql](../../sql/history/V006__create_device_model_snmp_point.sql)

---

## 1. 개요

| 개념 | 설명 |
|------|------|
| **DeviceModel** | 장비 제품 모델 (SKU/제품군) |
| **DeviceModelProtocol** | 모델 ↔ `PROTOCOL_TYPE` 연결. SNMP는 `protocolCode = snmp` |
| **DeviceModelSnmpPoint** | SNMP 프로토콜 연결 1건 아래의 **수집 point 정의** (OID 템플릿) |
| **Device** | 실제 장비. host/port는 [endpoint](../device/DEVICE_ENDPOINT_API.md), SNMP 전용 값은 확장 테이블(예정) |

SNMP 수집에 필요한 정보는 두 층으로 나뉩니다.

| 층 | 저장 위치 | 예시 |
|----|-----------|------|
| **모델 카탈로그** | `device_model_snmp_point` | point별 OID 패턴, `전압`, `V` |
| **장비 인스턴스** | `devices` + `device_protocol_endpoint` (+ snmp 확장 예정) | `192.168.1.10`, `161`, `instanceId = 2` |

모델에는 **name + OID(또는 OID 템플릿)** 를 미리 정의해 두고,  
장비 등록·스크립트 생성 시 `host`, `port`, `instanceId`를 합쳐 최종 OID를 만듭니다.

### 1.1 OID 패턴

같은 모델이라도 장비 연결 순서(MIB instance index)에 따라 OID가 달라지는 경우가 있습니다.

```text
고정 OID:     1.3.6.1.4.1.12345.10.1.0
템플릿 OID:   1.3.6.1.4.1.12345.{instanceId}.10.1.0
```

| 유형 | `requiresInstance` | `oid` 예시 | 장비 측 필요 값 |
|------|--------------------|------------|-----------------|
| 고정 | `false` | `1.3.6.1.4.1.12345.10.1.0` | 없음 |
| 템플릿 | `true` | `1.3.6.1.4.1.12345.{instanceId}.10.1.0` | `instanceId` |

스크립트 생성 시:

```text
if point.requiresInstance:
  finalOid = oid.replace("{instanceId}", device.instanceId)
else:
  finalOid = oid
```

### 1.2 `name` — 식별자이자 표시명

`name`은 **식별자이자 표시명**으로 사용합니다.

| 구분 | 예시 | 비고 |
|------|------|------|
| PDU 전압 | `V`, `전압` | 단순 point는 동일 문자열로 충분 |
| CDU 유량 | `PRI-FLOW`, `1차 유량` | 길이·언어는 자유 |
| 다른 모델과 동일 name | PDU 3상 `V`, PDU 단상 `V` | **다른 모델**이면 중복 허용 |

- **같은 모델·같은 SNMP protocol** 안에서만 `name` 유일 (UK)
- **같은 모델·같은 SNMP protocol** 안에서 `oid`도 유일 (UK, V012)
- 모델 간 `V` / 동일 OID 중복은 정상 (서로 다른 `model_protocol_id`)

### 1.3 boolean 컬럼 규칙 (프로젝트 공통)

플래그성 컬럼은 **`boolean` (`true` / `false`)** 을 사용합니다.  
본 테이블이 최초 적용 대상이며, 이후 Modbus/MQTT point·device 설정 등에도 동일 규칙을 적용합니다.

| 계층 | 규칙 | 예시 |
|------|------|------|
| DB | `TINYINT(1) NOT NULL DEFAULT 0` (또는 `1`) | `requires_instance`, `enabled` |
| Java | `boolean` + `@Column(nullable = false)` | `requiresInstance`, `enabled` |
| API JSON | `true` / `false` | `requiresInstance`, `enabled` |

> boolean 컬럼은 **여부 플래그**입니다. `instanceId` 실제 값(예: `3`)을 넣는 컬럼이 **아닙니다**.

| 컬럼 (DB) | API 필드 | 기본값 | 의미 |
|-----------|----------|--------|------|
| `requires_instance` | `requiresInstance` | `false` | OID를 그대로 사용. `{instanceId}` 치환 불필요 |
| `requires_instance` | `requiresInstance` | `true` | OID 템플릿의 `{instanceId}`를 장비 `instanceId`로 치환 |
| `enabled` | `enabled` | `true` | 수집·스크립트 생성 대상 |

**치환 불필요한 OID 예시**

```text
requires_instance = false (0)
oid               = 1.3.6.1.4.1.12345.10.1.0
```

**치환 필요한 OID 예시**

```text
requires_instance = true (1)
oid               = 1.3.6.1.4.1.12345.{instanceId}.10.1.0
device.instanceId = 3   ← `device_endpoint_snmp.instance_id` (예정)에 저장
→ 최종 OID: 1.3.6.1.4.1.12345.3.10.1.0
```

### 1.4 공통 제약

| 항목 | 규칙 |
|------|------|
| `modelId` | 존재하는 `device_model.id` |
| `protocolId` | 해당 모델 소속 `device_model_protocol.id` |
| 프로토콜 타입 | **SNMP만** 허용 (`protocolCode = snmp`) |
| `name` | 필수. `(model_protocol_id, name)` UK |
| `oid` | 필수. `(model_protocol_id, oid)` UK — 동일 protocol 내 OID 중복 불가 |
| `requiresInstance = true` | `oid`에 `{instanceId}` 포함 필수 |
| `requiresInstance = false` | `oid`에 placeholder 없어야 함 |
| 목록 정렬 | `id` 오름차순 |
| point 삭제 | 건별 삭제 허용 |

### 1.5 DeviceModel API와의 관계

| API | snmp point 처리 | 권장 |
|-----|-----------------|------|
| `POST/PUT /device-models` | `protocols[]`만 관리 (point 미포함) | 모델·프로토콜 연결 |
| **본 API (sub-resource)** | SNMP point **건별 CRUD** | point/OID 관리 **권장** |

- 모델에 SNMP 프로토콜을 연결한 뒤, 본 API로 point를 등록합니다.
- Modbus/MQTT point는 향후 각각 별도 sub-resource API로 분리합니다.

---

## 2. 테이블 — `device_model_snmp_point`

**구현 상태:** ✅ 구현 완료

| 컬럼 | 타입 | NULL | 키 | 기본값 | 설명 |
|------|------|------|-----|--------|------|
| `id` | INT | N | PK | AUTO_INCREMENT | point ID |
| `model_protocol_id` | INT | N | FK | | `device_model_protocol.id` |
| `name` | VARCHAR(255) | N | UK* | | 식별자·표시명 (`V`, `전압`, `PRI-FLOW`) |
| `oid` | VARCHAR(512) | N | UK** | | OID 또는 OID 템플릿 |
| `requires_instance` | TINYINT(1) | N | | `0` | `{instanceId}` 치환 필요 여부 (boolean) |
| `unit` | VARCHAR(50) | Y | | | 단위 (`V`, `A`, `L/min`) |
| `enabled` | TINYINT(1) | N | | `1` | 사용 여부 (boolean) |
| `created_dt` | TIMESTAMP(6) | Y | | | |
| `updated_dt` | TIMESTAMP(6) | Y | | | |

\* UK: `(model_protocol_id, name)`  
\*\* UK: `(model_protocol_id, oid)` — V012

**FK 제약**

| FK | 참조 | ON DELETE | ON UPDATE |
|----|------|-----------|-----------|
| `fk_device_model_snmp_point_model_protocol_id` | `device_model_protocol(id)` | CASCADE | CASCADE |

**관계**

```
device_model <- device_model_protocol -> common_code (snmp)
                        |
                        +-- device_model_snmp_point
```

## 3. 등록 API

### 3.1 point 추가 — `POST /api/manager/device-models/{modelId}/protocols/{protocolId}/snmp-points`

**구현 상태:** ✅ 구현 완료

#### 경로 파라미터

| 파라미터 | 설명 |
|----------|------|
| `modelId` | `device_model.id` |
| `protocolId` | `device_model_protocol.id` (해당 모델 소속, SNMP) |

#### 요청

```json
{
  "name": "PRI-FLOW",
  "oid": "1.3.6.1.4.1.12345.{instanceId}.10.1.0",
  "requiresInstance": true,
  "unit": "L/min",
  "enabled": true
}
```

| 필드 | 필수 | 타입 | 설명 |
|------|------|------|------|
| `name` | O | string | 식별자·표시명. 동일 protocol 내 유일 |
| `oid` | O | string | OID 또는 `{instanceId}` 포함 템플릿. 동일 protocol 내 유일 |
| `requiresInstance` | X | boolean | 기본 `false`. `true`면 `oid`에 `{instanceId}` 필수 |
| `unit` | X | string | 단위 |
| `enabled` | X | boolean | 기본 `true` |

#### 응답 — `201 Created`

```json
{
  "success": true,
  "data": {
    "id": 101,
    "modelId": 1,
    "protocolId": 10,
    "name": "PRI-FLOW",
    "oid": "1.3.6.1.4.1.12345.{instanceId}.10.1.0",
    "requiresInstance": true,
    "unit": "L/min",
    "enabled": true
  }
}
```

#### 오류

| 조건 | HTTP | 메시지(예) |
|------|------|------------|
| 모델 없음 | 404 | `DeviceModel not found: {modelId}` |
| protocol 없음 또는 소속 불일치 | 404 | `DeviceModelProtocol not found: {protocolId}` |
| SNMP가 아님 | 400 | `protocol must be snmp` |
| name 중복 | 400 | `point name already exists for this protocol` |
| oid 중복 | 400 | `point oid already exists for this protocol` |
| requiresInstance=true인데 placeholder 없음 | 400 | `oid must contain {instanceId}` |
| requiresInstance=false인데 placeholder 있음 | 400 | `oid must not contain {instanceId}` |
| oid 형식 오류 | 400 | `invalid oid format` |

### 3.2 point 일괄 추가 — `POST /api/manager/device-models/{modelId}/protocols/{protocolId}/snmp-points/bulk`

**구현 상태:** ✅ 구현 완료

여러 OID를 한 요청으로 등록합니다. 요청 내·DB 중복이 하나라도 있으면 **전부 롤백**하고, 수집 스크립트 재생성은 **1회**만 수행합니다.

#### 요청

```json
{
  "points": [
    {
      "name": "V",
      "oid": "1.3.6.1.4.1.xxx.1.1.0",
      "requiresInstance": false,
      "unit": "V",
      "enabled": true
    },
    {
      "name": "A",
      "oid": "1.3.6.1.4.1.xxx.1.2.0",
      "unit": "A"
    }
  ]
}
```

| 필드 | 필수 | 타입 | 설명 |
|------|------|------|------|
| `points` | O | array | 비어 있으면 400. 요소 스키마는 단건 등록과 동일 |
| `points[].name` / `oid` | O | string | 요청 배열 안에서도 name·oid 중복 불가 |

#### 응답 — `data` 배열

등록된 point 목록 (`id` 포함). 단건 응답 객체와 동일 스키마.

#### 오류

단건 등록 오류 + 아래:

| 조건 | HTTP | 메시지(예) |
|------|------|------------|
| `points` 비어 있음 | 400 | `points must not be empty` |
| 요청 내 name 중복 | 400 | `duplicate point name in request: {name}` |
| 요청 내 oid 중복 | 400 | `duplicate point oid in request: {oid}` |

---

## 4. 수정 API

### 4.1 point 수정 — `PUT /api/manager/device-models/{modelId}/protocols/{protocolId}/snmp-points/{pointId}`

**구현 상태:** ✅ 구현 완료

요청 body는 등록과 동일. **전체 교체** (부분 수정 없음).

#### 오류

등록 오류 + 아래 추가:

| 조건 | HTTP | 메시지(예) |
|------|------|------------|
| point 없음 또는 소속 불일치 | 404 | `DeviceModelSnmpPoint not found: {pointId}` |

---

## 5. 조회 API

### 5.1 목록 — `GET /api/manager/device-models/{modelId}/protocols/{protocolId}/snmp-points`

**구현 상태:** ✅ 구현 완료

`id` 오름차순. `enabled = false` point도 포함 (필터는 V2 검토).

#### 응답 — `200 OK`

```json
{
  "success": true,
  "data": [
    {
      "id": 101,
      "modelId": 1,
      "protocolId": 10,
      "name": "PRI-FLOW",
      "oid": "1.3.6.1.4.1.12345.{instanceId}.10.1.0",
      "requiresInstance": true,
      "unit": "L/min",
      "enabled": true
    }
  ]
}
```

### 5.2 단건 — `GET /api/manager/device-models/{modelId}/protocols/{protocolId}/snmp-points/{pointId}`

**구현 상태:** ✅ 구현 완료

목록 항목 1건과 동일 구조.

---

## 6. 삭제 API

### 6.1 point 삭제 — `DELETE /api/manager/device-models/{modelId}/protocols/{protocolId}/snmp-points/{pointId}`

**구현 상태:** ✅ 구현 완료

| 조건 | HTTP | 동작 |
|------|------|------|
| 존재 | 200 | 삭제, 삭제된 `id` 반환 |
| 모델/protocol/point 없음 | 404 | 각각 not found |

#### 응답 — `200 OK`

```json
{
  "success": true,
  "data": 101
}
```

---

## 7. API 요약

| Method | Path | 설명 | 상태 |
|--------|------|------|------|
| `GET` | `.../snmp-points` | SNMP point 목록 | ✅ |
| `GET` | `.../snmp-points/{pointId}` | SNMP point 단건 | ✅ |
| `POST` | `.../snmp-points` | SNMP point 추가 | ✅ |
| `POST` | `.../snmp-points/bulk` | SNMP point 일괄 추가 | ✅ |
| `PUT` | `.../snmp-points/{pointId}` | SNMP point 수정 | ✅ |
| `DELETE` | `.../snmp-points/{pointId}` | SNMP point 삭제 | ✅ |

---

## 8. 예시

### 8.1 PDU — 단순 point

| name | oid | requires_instance | unit | enabled |
|------|-----|-------------------|------|---------|
| `V` | `1.3.6.1.4.1.xxx.1.1.0` | `false` | `V` | `true` |
| `A` | `1.3.6.1.4.1.xxx.1.2.0` | `false` | `A` | `true` |
| `kW` | `1.3.6.1.4.1.xxx.1.3.0` | `false` | `kW` | `true` |

### 8.2 CDU — `IRCR01K41CDU`

OID 숫자는 **예시**이며, 실제 vendor MIB에 맞게 조정합니다.

| name | oid (템플릿) | requires_instance |
|------|----------------|-------------------|
| `PRI-FLOW` | `1.3.6.1.4.1.12345.{instanceId}.1.1.0` | `true` |
| `PRI-RET-PRES` | `1.3.6.1.4.1.12345.{instanceId}.1.2.0` | `true` |
| `PRI-RET-TEMP` | `1.3.6.1.4.1.12345.{instanceId}.1.3.0` | `true` |
| `PRI-SUP-PRES` | `1.3.6.1.4.1.12345.{instanceId}.1.4.0` | `true` |
| `PRI-SUP-TEMP` | `1.3.6.1.4.1.12345.{instanceId}.1.5.0` | `true` |
| `SEC-FLOW` | `1.3.6.1.4.1.12345.{instanceId}.2.1.0` | `true` |
| `SEC-RET-PRES` | `1.3.6.1.4.1.12345.{instanceId}.2.2.0` | `true` |
| `SEC-RET-TEMP` | `1.3.6.1.4.1.12345.{instanceId}.2.3.0` | `true` |
| `SEC-SUP-PRES` | `1.3.6.1.4.1.12345.{instanceId}.2.4.0` | `true` |
| `SEC-SUP-TEMP` | `1.3.6.1.4.1.12345.{instanceId}.2.5.0` | `true` |

장비 `instanceId = 3`일 때 `PRI-FLOW` 최종 OID:

```text
1.3.6.1.4.1.12345.3.1.1.0
```

---

## 9. 스크립트 생성 (향후)

본 API 데이터는 **자동 수집 스크립트 생성기**의 입력으로 사용합니다.

```
입력:
  - device_model_snmp_point[]  (id 순)
  - endpoint.host, endpoint.port
  - snmp.instanceId (device_endpoint_snmp, 예정)

처리:
  for point in points where enabled:
    oid = point.oid
    if point.requiresInstance:
      oid = oid.replace("{instanceId}", snmp.instanceId)
    emit collect(host, port, oid, point.name)

출력:
  - 수집 스크립트 / 설정 JSON
```

V2에서 `GET .../snmp-points/script-template?deviceId=` export API 검토.

---

## 10. device 모듈 연동

> 장비 본체: [DEVICE_API.md](../device/DEVICE_API.md)  
> host/port 공통 전송층: [DEVICE_ENDPOINT_API.md](../device/DEVICE_ENDPOINT_API.md) (V009 ✅)  
> SNMP 전용 확장: `device_endpoint_snmp` (예정)

| 항목 | 저장 위치 | 상태 |
|------|-----------|------|
| `host`, `port` | `device_protocol_endpoint` | ✅ |
| `instanceId` | `device_endpoint_snmp.instance_id` | ⬜ 예정 |
| `community`, `version` | `device_endpoint_snmp` | ⬜ 예정 |

모델 API는 **카탈로그(무엇을 어떤 OID로 읽을지)** 만 담당하고,  
연결 정보·치환 값은 device 모듈(endpoint + 확장)에서 관리합니다.  
(`device_snmp_config` 단독 테이블은 쓰지 않음 — [DEVICE_ARCHITECTURE §2](../device/DEVICE_ARCHITECTURE.md))

---

## 11. 구현 순서 (권장)

1. `V006__create_device_model_snmp_point.sql`
2. `DeviceModelSnmpPoint` 엔티티 + validation
3. `DeviceModelSnmpPointRepository` + QueryService
4. `DeviceModelSnmpPointController` + DTO
5. 통합 테스트 (CRUD, SNMP 외 protocol 거부, OID 템플릿·boolean 검증)
6. [ERD.md](../ERD.md) 갱신
7. (선택) CDU 10 point 시드 또는 관리 화면 연동

---

## 12. 구현 현황

| 구분 | 내용 |
|------|------|
| 문서 | 본 문서 |
| DDL | V006 ✅ |
| 도메인 | `DeviceModelSnmpPoint` ✅ |
| Application | `DeviceModelSnmpPointQueryService` ✅ |
| API | sub-resource CRUD ✅ |
| device 연동 | host/port — ✅ endpoint / instanceId·community — ⬜ snmp 확장 |
| script export | — 미구현 |

---

## 13. 갱신 이력

| 날짜 | 변경 |
|------|------|
| 2026-07-10 | 최초 작성 |
| 2026-07-21 | CRUD API 구현 완료 (등록·수정·목록·단건·삭제) |
| 2026-07-23 | device 연동 경로를 `device_protocol_endpoint`(+ snmp 확장)로 정정 (`device_snmp_config` 폐기) |