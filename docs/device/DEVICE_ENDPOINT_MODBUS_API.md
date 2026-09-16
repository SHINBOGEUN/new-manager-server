# Device Endpoint Modbus API 설계

`device` 모듈의 **Modbus 엔드포인트 확장**(`device_endpoint_modbus`) API·비즈니스 규칙을 정리한 문서입니다.

> API prefix: `/api/manager/devices/{deviceId}/endpoints/{endpointId}/modbus`  
> 아키텍처: [DEVICE_ARCHITECTURE.md](./DEVICE_ARCHITECTURE.md)  
> 부모 API: [DEVICE_ENDPOINT_API.md](./DEVICE_ENDPOINT_API.md)  
> 모델 카탈로그: [DEVICE_MODEL_MODBUS_POINT_API.md](../devicemodel/DEVICE_MODEL_MODBUS_POINT_API.md)  
> 대칭 구조(SNMP): [DEVICE_SNMP_INSTANCE_API.md](./DEVICE_SNMP_INSTANCE_API.md)  
> DDL: [`23_device_endpoint_modbus.sql`](../../sql/schema/23_device_endpoint_modbus.sql)

---

## 1. 개요

| 개념 | 설명 |
|------|------|
| **DeviceProtocolEndpoint** | host/port (Modbus TCP 접속 주소). [DEVICE_ENDPOINT_API](./DEVICE_ENDPOINT_API.md) |
| **DeviceEndpointModbus** | Modbus **unit/slave ID** (endpoint당 0~1행) |
| **DeviceModelModbusPoint** | 모델 point의 레지스터 주소·타입·배율. `requiresInstance=true`이면 주소를 인스턴스가 제공 |

Modbus 장비 식별은 **`host` + `port` + `unit_id`** 3요소로 이뤄집니다.
앞의 둘은 모든 프로토콜 공통이라 부모 테이블에 있고, `unit_id`만 이 테이블이 담당합니다.

| 층 | 저장 위치 | 예시 |
|----|-----------|------|
| **모델 카탈로그** | `device_model_modbus_point` | `IN_TEMP` = INPUT 1번, INT16, ×0.1 |
| **접속 주소** | `device_protocol_endpoint` | `192.168.10.21:502` |
| **unit** | `device_endpoint_modbus` | `unitId = 1` |

### 1.1 SNMP와의 대칭

| 역할 | SNMP | Modbus |
|------|------|--------|
| 카탈로그 | `device_model_snmp_point` | `device_model_modbus_point` |
| 접속 (공통) | `device_protocol_endpoint` | `device_protocol_endpoint` |
| 프로토콜 확장 | `device_snmp_instance` | **`device_endpoint_modbus`** |
| 가변 주소 표현 | OID의 `{instanceId}` 치환 | `requires_instance` 플래그 |
| 결과 라우팅 | 불필요 | `device_modbus_reading` (등록·수정·단건 삭제 구현) |

### 1.2 이번 범위

| 포함 | 미포함 (이후) |
|------|----------------|
| `device_endpoint_modbus` CRUD (endpoint 1:1) | `device_modbus_reading` 회선별 매핑 |
| Modbus endpoint만 허용 | Modbus 수집 스크립트 생성 |
| `unitId` 0~247 또는 NULL | Ops Console UI |

**행이 없는 것은 정상**입니다. 아직 현장 unit 번호를 모를 때는 등록하지 않습니다.

### 1.3 `unitId`가 선택(nullable)인 이유

SNMP의 `instanceId`는 필수(≥1)지만, Modbus의 `unitId`는 **NULL을 허용**합니다.
장비 유형에 따라 unit 번호가 있는 자리가 다르기 때문입니다.

| 장비 유형 | `requires_instance` | `unitId` | 비고 |
|-----------|---------------------|----------|------|
| RDC 등 단순 장비 | `0` (주소 고정) | **값 지정** | 이 한 값으로 수집 가능 |
| ACCURA 등 분전반 | `1` (주소 가변) | **`NULL`** | 회선마다 달라 `device_modbus_reading`이 담당 |

> ⚠️ SNMP 서비스에는 "모델에 `requires_instance` point가 없으면 400" 검증이 있지만,
> **Modbus에는 그 검증을 두지 않습니다.** `unit_id`는 `requires_instance` 값과 무관하게
> 접속 자체에 필요하기 때문입니다. (RDC는 `requires_instance=0`이지만 `unit_id`가 반드시 필요)

### 1.4 공통 제약

| 항목 | 규칙 |
|------|------|
| `deviceId` | 존재하는 `devices.id` |
| `endpointId` | 해당 `deviceId` 소속 `device_protocol_endpoint.id` |
| 프로토콜 | endpoint의 `protocolCode` = **`modbus`** |
| UK | `endpoint_id` PK — Modbus endpoint당 **최대 1행** |
| `unitId` | 선택. NULL이거나 **0~247** |
| endpoint 삭제 | modbus 행과 소속 reading **CASCADE** |
| modbus 설정 삭제 | 소속 reading **CASCADE**, 공통 endpoint 유지 |

---

## 2. 테이블 — `device_endpoint_modbus`

**구현 상태:** ✅ DDL / CRUD 구현 완료

| 컬럼 | 타입 | NULL | 키 | 설명 |
|------|------|------|-----|------|
| `endpoint_id` | INT | N | PK, FK | `device_protocol_endpoint.id` |
| `unit_id` | INT | Y | | unit/slave ID (CHECK NULL 또는 0~247) |
| `created_dt` | TIMESTAMP(6) | Y | | |
| `updated_dt` | TIMESTAMP(6) | Y | | |

**FK:** `endpoint_id` → `device_protocol_endpoint(id)` **ON DELETE CASCADE**

**JPA:** `@OneToOne` + `@MapsId` — 자식 PK가 곧 부모 PK (별도 `@GeneratedValue` 없음)

**관계도**

```mermaid
erDiagram
    devices ||--o{ device_protocol_endpoint : device_id
    device_protocol_endpoint ||--o| device_endpoint_modbus : endpoint_id
    device_protocol_endpoint ||--o| device_snmp_instance : endpoint_id

    device_protocol_endpoint {
        int id PK
        int device_id FK
        varchar host
        int port
    }

    device_endpoint_modbus {
        int endpoint_id PK_FK
        int unit_id
    }

    device_snmp_instance {
        int endpoint_id PK_FK
        int instance_id
    }
```

---

## 3. 등록 API

### 3.1 등록 — `POST /api/manager/devices/{deviceId}/endpoints/{endpointId}/modbus`

**구현 상태:** ✅ 구현됨

#### 요청

```json
{
  "unitId": 1
}
```

| 필드 | 필수 | 설명 |
|------|------|------|
| `unitId` | X | 0~247. 회선별로 다르면 생략 또는 `null` |

#### 응답 — `200 OK`

```json
{
  "success": true,
  "data": {
    "endpointId": 301,
    "deviceId": 101,
    "unitId": 1
  }
}
```

| 조건 | HTTP | 메시지(예) |
|------|------|------------|
| device 없음 | 404 | `Device not found: {deviceId}` |
| endpoint 없음 | 404 | `DeviceProtocolEndpoint not found: {endpointId}` |
| endpoint가 Modbus 아님 | 400 | `endpoint protocol must be modbus` |
| 이미 등록됨 | 409 | `modbus endpoint already exists for this endpoint` |
| `unitId` 범위 밖 | 400 | Bean validation — `unitId must be greater than or equal to 0` / `unitId must be less than or equal to 247` (엔티티 방어: `unitId must be between 0 and 247`) |

---

## 4. 수정 API

### 4.1 수정 — `PUT /api/manager/devices/{deviceId}/endpoints/{endpointId}/modbus`

**구현 상태:** ✅

요청 body는 등록과 동일 (`unitId` 전체 교체).

| 조건 | HTTP | 메시지(예) |
|------|------|------------|
| modbus 행 없음 | 404 | `DeviceEndpointModbus not found for endpoint: {endpointId}` |
| (그 외) | | 등록 API와 동일 |

---

## 5. 조회 API

### 5.1 단건 — `GET /api/manager/devices/{deviceId}/endpoints/{endpointId}/modbus`

**구현 상태:** ✅

| 조건 | HTTP | 동작 |
|------|------|------|
| 존재 | 200 | `endpointId`, `deviceId`, `unitId` |
| device/endpoint 없음 | 404 | (endpoint API와 동일) |
| modbus 미등록 | 404 | `DeviceEndpointModbus not found for endpoint: {endpointId}` |

> 설정이 **선택**이므로 404는 오류가 아니라 "아직 설정 안 함"으로 UI에서 처리.

---

## 6. 삭제 API

### 6.1 삭제 — `DELETE /api/manager/devices/{deviceId}/endpoints/{endpointId}/modbus`

설정과 소속 `device_modbus_reading`을 DB FK의 `ON DELETE CASCADE`로 함께 삭제합니다.
reading의 `endpoint_id`는 `device_endpoint_modbus.endpoint_id`를 참조합니다.
공통 endpoint, 모델 point, 대상 장비와 기존 Influx 데이터는 삭제하지 않습니다.
reading 단건 삭제는 `DELETE .../modbus/readings/{readingId}`이며 부모 설정은 유지됩니다.
두 DELETE 모두 body는 없습니다. 운영 데이터 대신 테스트 장비로 검증합니다.

**구현 상태:** ✅

| 조건 | HTTP | 동작 |
|------|------|------|
| 존재 | 200 | 삭제, `endpointId` 반환 |
| 없음 | 404 | `DeviceEndpointModbus not found for endpoint: {endpointId}` |

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
| `GET` | `.../endpoints/{endpointId}/modbus` | 단건 조회 | ✅ |
| `POST` | `.../endpoints/{endpointId}/modbus` | 등록 | ✅ |
| `PUT` | `.../endpoints/{endpointId}/modbus` | 수정 | ✅ |
| `DELETE` | `.../endpoints/{endpointId}/modbus` | 삭제 | ✅ |

목록 API는 **없음** (endpoint당 0~1행).

---

## 8. 데이터 예시

### 8.1 RDC 2대 — 단순 장비 (`requires_instance=0`)

**모델** RDC — 주소가 카탈로그에 고정

| name | register_type | data_type | address | requires_instance | scale |
|------|---------------|-----------|---------|-------------------|-------|
| `IN_TEMP` | INPUT | INT16 | 1 | 0 | 0.1 |
| `POWER` | HOLDING | FLOAT32 (CDAB) | 3 | 0 | 1000 |

**장비 101 RDC-A**

| 테이블 | 값 |
|--------|-----|
| `device_protocol_endpoint` | `192.168.10.21:502` |
| `device_endpoint_modbus` | `unitId = 1` |

**장비 102 RDC-B**

| 테이블 | 값 |
|--------|-----|
| `device_protocol_endpoint` | `192.168.10.22:502` |
| `device_endpoint_modbus` | `unitId = 1` |

주소 `1`, `3`은 카탈로그에 한 번만 정의되고 2대가 공유합니다.
장비가 늘어도 카탈로그는 그대로, 인스턴스 행만 추가됩니다.

### 8.2 ACCURA — 분전반 (`requires_instance=1`, 본 API 범위 밖)

보드 1대가 **여러 장비의 전력**을 재는 경우입니다.

| 테이블 | 값 |
|--------|-----|
| `device_protocol_endpoint` | `192.168.10.30:502` |
| `device_endpoint_modbus` | `unitId = NULL` |
| `device_modbus_reading` (⬜) | `unit 1 → device 101 POWER`, `unit 3 → device 102 POWER` |

회선마다 unit·주소·대상 장비가 달라서 이 테이블 한 행으로 표현할 수 없습니다.
`device_modbus_reading`의 등록·수정·단건 삭제가 구현되어 있습니다. 수집 스크립트 생성은 별도 작업입니다.

---

## 9. 알려진 제약

| 항목 | 내용 |
|------|------|
| 게이트웨이 공유 | `device_protocol_endpoint`에 `UK(host, port)`가 있어 **여러 장비가 같은 host:port를 공유할 수 없음**. 현재 현장(RDC는 각자 IP)에서는 문제 없으나, 게이트웨이 뒤 다중 slave 장비가 들어오면 UK 완화 필요 |
| endpoint 1개 제한 | `UK(device_id, protocol_type_id)` — 한 장비는 프로토콜당 endpoint 1개. ACCURA가 남의 전력을 잴 때 두 번째 endpoint를 달 수 없어 매핑 테이블이 **필수** |
| 수집 미연동 | `CollectionGroupSpecService`가 `scriptType=modbus`를 아직 지원하지 않아, `unitId`를 저장해도 수집 스크립트가 생성되지 않음 → [BACKLOG](../BACKLOG.md) 1.8 |

---

## 10. 구현 현황

| 구분 | 내용 |
|------|------|
| 문서 | 본 문서 ✅ |
| DDL | `23_device_endpoint_modbus.sql` ✅ |
| 도메인 | `DeviceEndpointModbus` ✅ |
| Repository | `DeviceEndpointModbusRepository` + JPA/SpringData ✅ |
| Application | `DeviceEndpointModbusQueryService` (CRUD) ✅ |
| API | 조회·등록·수정·삭제 ✅ |
| 엔티티 테스트 | ⬜ |
| 통합 테스트 | ⬜ |

---

## 11. 갱신 이력

| 날짜 | 변경 |
|------|------|
| 2026-09-03 | 최초 작성 — `23_device_endpoint_modbus.sql`, endpoint 1:1 CRUD |
| 2026-09-16 | Modbus 설정 → reading FK 및 연쇄 삭제 정책 반영 |
