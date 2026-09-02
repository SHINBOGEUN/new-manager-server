# Device SNMP Instance API 설계

`device` 모듈의 **SNMP instance 인덱스**(`device_snmp_instance`) API·비즈니스 규칙을 정리한 문서입니다.

> API prefix: `/api/manager/devices/{deviceId}/endpoints/{endpointId}/snmp-instance`  
> 아키텍처: [DEVICE_ARCHITECTURE.md](./DEVICE_ARCHITECTURE.md) §2.2  
> 부모 API: [DEVICE_ENDPOINT_API.md](./DEVICE_ENDPOINT_API.md)  
> 모델 카탈로그: [DEVICE_MODEL_SNMP_POINT_API.md](../devicemodel/DEVICE_MODEL_SNMP_POINT_API.md)  
> DDL: [`11_device_snmp_instance.sql`](../../sql/schema/11_device_snmp_instance.sql)

---

## 1. 개요

| 개념 | 설명 |
|------|------|
| **DeviceProtocolEndpoint** | host/port (SNMP 접속 주소). [DEVICE_ENDPOINT_API](./DEVICE_ENDPOINT_API.md) |
| **DeviceSnmpInstance** | SNMP OID 템플릿 `{instanceId}` **치환값** (endpoint당 0~1행) |
| **DeviceModelSnmpPoint** | 모델 point OID 패턴. `requiresInstance=true`이면 장비 instance 필요 |

PDU 등 **TEMPLATE형** 모델은 모델 point가 `{instanceId}` placeholder를 쓰고,  
**같은 장비의 모든 requires_instance point가 동일 instance 숫자**를 공유합니다.

| 층 | 저장 위치 | 예시 |
|----|-----------|------|
| **모델 카탈로그** | `device_model_snmp_point` | `1.3.6.1...{instanceId}...3` (전압), `...4` (전류) |
| **접속 주소** | `device_protocol_endpoint` | `192.168.1.10:161` |
| **instance** | `device_snmp_instance` | `instanceId = 1` |

### 1.1 이번 범위

| 포함 | 미포함 (이후) |
|------|----------------|
| `device_snmp_instance` CRUD (endpoint 1:1) | `community`, `version` (앱 기본값) |
| SNMP endpoint만 허용 | Modbus/MQTT |
| `instanceId` ≥ 1 | SRC형 **장비별 OID 전체** → `device_snmp_point` (별도) |

**행이 없는 것은 정상**입니다. 모델 point가 전부 고정 OID(`requiresInstance=false`)이거나, 아직 현장 index를 모를 때는 등록하지 않습니다.

### 1.2 community / version

SNMP `community`, `version`은 **DB·API에 두지 않습니다.**  
애플리케이션 설정 기본값(예: `public`, `2c`)을 사용합니다. 현장별로 달라지면 이후 설정 또는 별도 컬럼 검토.

### 1.3 공통 제약

| 항목 | 규칙 |
|------|------|
| `deviceId` | 존재하는 `devices.id` |
| `endpointId` | 해당 `deviceId` 소속 `device_protocol_endpoint.id` |
| 프로토콜 | endpoint의 `protocolCode` = **`snmp`** |
| UK | `endpoint_id` PK — SNMP endpoint당 **최대 1행** |
| `instanceId` | 필수. **1 이상** 정수 |
| endpoint 삭제 | instance 행 **CASCADE** |

### 1.4 모델 point와의 관계

```text
모델 point (전압):  1.3.6.1.4.1.318...{instanceId}...3
모델 point (전류):  1.3.6.1.4.1.318...{instanceId}...4
장비 instance:      1

→ 전압 OID: ...1...3
→ 전류 OID: ...1...4   (point마다 행을 두지 않음)
```

모델에 `requires_instance=true` point가 **하나도 없으면** instance API 호출은 **불필요**(400 권장).

---

## 2. 테이블 — `device_snmp_instance`

**구현 상태:** ✅ DDL (V011) / CRUD 구현 완료

| 컬럼 | 타입 | NULL | 키 | 설명 |
|------|------|------|-----|------|
| `endpoint_id` | INT | N | PK, FK | `device_protocol_endpoint.id` |
| `instance_id` | INT | N | | `{instanceId}` 치환값 (CHECK ≥ 1) |
| `created_dt` | TIMESTAMP(6) | Y | | |
| `updated_dt` | TIMESTAMP(6) | Y | | |

**FK:** `endpoint_id` → `device_protocol_endpoint(id)` **ON DELETE CASCADE**

**관계도**

```mermaid
erDiagram
    devices ||--o{ device_protocol_endpoint : device_id
    device_protocol_endpoint ||--o| device_snmp_instance : endpoint_id

    device_protocol_endpoint {
        int id PK
        int device_id FK
        varchar host
        int port
    }

    device_snmp_instance {
        int endpoint_id PK_FK
        int instance_id
    }
```

---

## 3. 등록 API

### 3.1 등록 — `POST /api/manager/devices/{deviceId}/endpoints/{endpointId}/snmp-instance`

**구현 상태:** ✅ 구현됨

#### 요청

```json
{
  "instanceId": 1
}
```

| 필드 | 필수 | 설명 |
|------|------|------|
| `instanceId` | O | 1 이상. MIB instance index |

#### 응답 — `200 OK`

```json
{
  "success": true,
  "data": {
    "endpointId": 301,
    "deviceId": 101,
    "instanceId": 1
  }
}
```

| 조건 | HTTP | 메시지(예) |
|------|------|------------|
| device 없음 | 404 | `Device not found: {deviceId}` |
| endpoint 없음 | 404 | `DeviceProtocolEndpoint not found: {endpointId}` |
| endpoint가 SNMP 아님 | 400 | `endpoint protocol must be snmp` |
| 모델에 requires_instance point 없음 | 400 | `device model has no snmp point requiring instance` |
| 이미 등록됨 | 409 | `snmp instance already exists for this endpoint` |
| `instanceId` &lt; 1 | 400 | Bean validation |

---

## 4. 수정 API

### 4.1 수정 — `PUT /api/manager/devices/{deviceId}/endpoints/{endpointId}/snmp-instance`

**구현 상태:** ✅

요청 body는 등록과 동일 (`instanceId` 전체 교체).

| 조건 | HTTP | 메시지(예) |
|------|------|------------|
| instance 없음 | 404 | `DeviceSnmpInstance not found for endpoint: {endpointId}` |
| (그 외) | | 등록 API와 동일 |

---

## 5. 조회 API

### 5.1 단건 — `GET /api/manager/devices/{deviceId}/endpoints/{endpointId}/snmp-instance`

**구현 상태:** ✅

| 조건 | HTTP | 동작 |
|------|------|------|
| 존재 | 200 | `endpointId`, `deviceId`, `instanceId` |
| device/endpoint 없음 | 404 | (endpoint API와 동일) |
| instance 미등록 | 404 | `DeviceSnmpInstance not found for endpoint: {endpointId}` |

> instance가 **선택**이므로 404는 오류가 아니라 “아직 설정 안 함”으로 UI에서 처리.

---

## 6. 삭제 API

### 6.1 삭제 — `DELETE /api/manager/devices/{deviceId}/endpoints/{endpointId}/snmp-instance`

**구현 상태:** ✅

| 조건 | HTTP | 동작 |
|------|------|------|
| 존재 | 200 | 삭제, `endpointId` 반환 |
| 없음 | 404 | `DeviceSnmpInstance not found for endpoint: {endpointId}` |

#### 응답 — `200 OK`

```json
{
  "success": true,
  "data": 301
}
```

---

## 7. API 요약

| Method | Path | 설명 | 상태 |
|--------|------|------|------|
| `GET` | `.../endpoints/{endpointId}/snmp-instance` | 단건 조회 | ✅ |
| `POST` | `.../endpoints/{endpointId}/snmp-instance` | 등록 | ✅ |
| `PUT` | `.../endpoints/{endpointId}/snmp-instance` | 수정 | ✅ |
| `DELETE` | `.../endpoints/{endpointId}/snmp-instance` | 삭제 | ✅ |

목록 API는 **없음** (endpoint당 0~1행).

---

## 8. 데이터 예시 (PDU)

**모델** AP8959 — point 2개, 둘 다 `{instanceId}` 사용

| name | oid |
|------|-----|
| V | `1.3.6.1.4.1.318...{instanceId}...3` |
| A | `1.3.6.1.4.1.318...{instanceId}...4` |

**장비 101 PDU-좌**

| 테이블 | 값 |
|--------|-----|
| `device_protocol_endpoint` | `192.168.1.10:161` |
| `device_snmp_instance` | `instanceId = 1` |

**장비 102 PDU-우**

| `device_snmp_instance` | `instanceId = 11` |

---

## 9. SRC 등 다른 유형 (본 API 범위 밖)

배선에 따라 **같은 논리 point(습도)의 OID 경로 자체가 달라지는** 장비는 `{instanceId}` 치환으로 해결되지 않습니다.  
이 경우 **`device_snmp_point`** (장비·point별 전체 OID) 별도 설계 예정. 본 테이블/API와 혼동하지 않습니다.

---

## 10. 구현 현황

| 구분 | 내용 |
|------|------|
| 문서 | 본 문서 |
| DDL | V011 ✅ |
| 도메인 | `DeviceSnmpInstance` ✅ |
| Application | `DeviceSnmpInstanceQueryService.createSnmpInstance` ✅ |
| API | 등록 `POST` ✅ / 조회·수정·삭제 ⬜ |
| 통합 테스트 | 등록 ✅ |

---

## 11. 갱신 이력

| 날짜 | 변경 |
|------|------|
| 2026-07-28 | 최초 작성 — V011, endpoint 1:1 CRUD 설계 |
