# Device Modbus Reading API 설계

`device` 모듈의 **Modbus 회선 매핑**(`device_modbus_reading`) API·비즈니스 규칙을 정리한 문서입니다.

> API prefix: `/api/manager/devices/{deviceId}/endpoints/{endpointId}/modbus/readings`  
> 부모 API: [DEVICE_ENDPOINT_MODBUS_API.md](./DEVICE_ENDPOINT_MODBUS_API.md)  
> 모델 카탈로그: [DEVICE_MODEL_MODBUS_POINT_API.md](../devicemodel/DEVICE_MODEL_MODBUS_POINT_API.md)  
> DDL: [`43_device_modbus_reading.sql`](../../sql/schema/43_device_modbus_reading.sql) · 기존 DB용 [`44_alter_modbus_reading_endpoint.sql`](../../sql/schema/44_alter_modbus_reading_endpoint.sql)

---

## 1. 개요

### 1.1 왜 필요한가

Modbus 장비 대부분은 **한 대가 자기 값을 자기 이름으로 보고**합니다. RDC 냉각기는 자기 IP에서 온도·팬 9개를 읽고 전부 자기 `device_id`로 기록합니다. 이 경우 모델 카탈로그(`device_model_modbus_point`)에 주소를 고정하고, 장비에는 접속 정보(`device_endpoint_modbus.unit_id`)만 두면 끝납니다.

**분전반(ACCURA)은 다릅니다.** 보드 한 대가 여러 회선을 갖고, 각 회선이 **다른 장비의 전력**을 잽니다.

```
IRCACCURA 보드 (14.42.43.207:30500)
  ├─ unit 0, 주소 11265 → IRCACCURA 자신의 TOTAL_WT
  ├─ unit 1, 주소 11415 → CHILLER 의 TOTAL_WT        ← 남의 장비
  ├─ unit 2, 주소 11565 → IRCCOOLER 의 TOTAL_WT      ← 남의 장비
  ├─ unit 3, 주소 11715 → IRCACCURA 의 WC-FAN-TOTAL_WT
  └─ unit 7·8·9        → IRCACCURA 의 LIQ-LOAD / IMC1 / IMC2
```

회선마다 **unit·주소·기록할 장비·필드명**이 전부 다르므로 카탈로그에 담을 수 없고, 장비 1대 = unit 1개인 endpoint 구조로도 표현이 안 됩니다. `device_modbus_reading`은 이 회선 하나하나를 행으로 담는 **매핑 테이블**입니다.

### 1.2 개념

| 개념 | 설명 |
|------|------|
| **DeviceEndpointModbus** | Modbus 접속 설정 (endpoint 1:1). reading의 **소유자** |
| **DeviceModbusReading** | 회선 1개 = 행 1개. "어느 unit·주소를 읽어 어느 장비의 어느 이름으로 기록하나" |
| **DeviceModelModbusPoint** | 해석 레시피 (register_type·data_type·byte_order·scale). `requiresInstance=true`인 point만 reading이 참조 |
| **targetDevice** | 결과를 기록할 `devices` 행. 원본 장비 자신이어도, 다른 장비여도 됨 |

### 1.3 계층 구조

```
device_protocol_endpoint        host:port           (모든 프로토콜 공통)
  └─ device_endpoint_modbus     unit_id             (Modbus 확장, 1:1)
       └─ device_modbus_reading  회선 × N           (이 문서)
```

reading은 `device_endpoint_modbus`의 **자식**입니다. 부모가 삭제되면 CASCADE로 함께 삭제되고, 부모 없이는 존재할 수 없습니다(FK). URL 경로 `/endpoints/{ep}/modbus/readings`가 이 계층을 그대로 반영합니다.

### 1.4 카탈로그와 인스턴스의 분담

| 정보 | 어디에 | 이유 |
|------|--------|------|
| register_type, data_type, byte_order, scale | `device_model_modbus_point` (카탈로그) | 회선 7개가 전부 같은 해석 방식 — 한 번만 정의 |
| unit_id, address | `device_modbus_reading` (인스턴스) | 회선마다 다름 |
| target_device_id, point_name | `device_modbus_reading` (인스턴스) | 회선마다 다름 |

카탈로그 point는 `requires_instance=1`, `address=NULL`로 등록해 "주소는 인스턴스가 준다"고 표시합니다. reading이 그 자리를 채웁니다.

### 1.5 공통 제약

| 항목 | 규칙 |
|------|------|
| `deviceId` | 존재하는 `devices.id` (수집 **원본** 장비) |
| `endpointId` | 해당 `deviceId` 소속 `device_protocol_endpoint.id` |
| 프로토콜 | endpoint의 `protocolCode` = **`modbus`** |
| Modbus 설정 | `device_endpoint_modbus` 행이 있어야 함 (없으면 404) |
| `pointId` | 원본 장비 모델의 modbus point, **`requires_instance=1`** |
| `unitId` | 0~247 |
| `address` | 0~65535, `address + registerCount − 1 ≤ 65535` |
| `targetDeviceId` | 존재하는 `devices.id`. 자기 자신 허용 |
| `pointName` | 필수. UK `(target_device_id, point_name)` |
| `enabled` | 등록 시 선택(기본 true), 수정 시 필수 |

**endpoint 검증 규칙은 5개 API에 동일하게 적용됩니다.** endpoint 프로토콜이 modbus가 아니거나 Modbus 설정 행이 없으면 조회·등록·수정·삭제 모두 거부합니다. 계층 구조상 무효 상태의 정리는 부모 삭제(CASCADE)로 하므로, reading을 개별로 다룰 필요가 없습니다.

---

## 2. 테이블 — `device_modbus_reading`

**구현 상태:** ✅ DDL / CRUD 구현 완료

| 컬럼 | 타입 | NULL | 키 | 설명 |
|------|------|------|-----|------|
| `id` | INT | N | PK | AUTO_INCREMENT |
| `endpoint_id` | INT | N | FK | `device_endpoint_modbus.endpoint_id` — 소유자 |
| `point_id` | INT | N | FK | `device_model_modbus_point.id` — 해석 레시피 |
| `unit_id` | INT | N | | 회선 slave ID (CHECK 0~247) |
| `address` | INT | N | | 실제 요청 시작 주소 (CHECK 0~65535) |
| `target_device_id` | INT | N | FK | `devices.id` — 결과 기록 장비 (Influx `device_id` 태그) |
| `point_name` | VARCHAR(255) | N | | 회선별 식별자·표시명 (Influx `point_name` 태그) |
| `enabled` | TINYINT(1) | N | | 기본 1 |
| `created_dt` / `updated_dt` | TIMESTAMP(6) | Y | | |

**UK:** `(target_device_id, point_name)` — 한 장비의 같은 이름에 두 회선이 덮어쓰는 것 방지

### 2.1 FK 정책

| FK | ON DELETE | 이유 |
|----|-----------|------|
| `endpoint_id` → `device_endpoint_modbus` | **CASCADE** | 소유자. Modbus 설정이 없어지면 회선 매핑은 존재할 이유가 없음 |
| `point_id` → `device_model_modbus_point` | **RESTRICT** | 참조. 카탈로그 화면에서 point를 지우는 사람은 현장 배선이 사라지는 걸 모름 |
| `target_device_id` → `devices` | **RESTRICT** | 참조. 장비 목록에서 지우는 사람은 남의 매핑 대상인지 모름 |

소유자는 CASCADE, 참조는 RESTRICT — `page_widget_device`, `pue_definition_source`와 같은 규칙입니다.

### 2.2 연쇄 삭제 범위

| 삭제 | 함께 삭제되는 것 |
|------|------------------|
| `device_protocol_endpoint` | `device_endpoint_modbus` → `device_modbus_reading` 전부 |
| `device_endpoint_modbus` | `device_modbus_reading` 전부 |
| `device_modbus_reading` 한 건 | 없음 (부모 유지) |

**관계도**

```mermaid
erDiagram
    devices ||--o{ device_protocol_endpoint : device_id
    device_protocol_endpoint ||--o| device_endpoint_modbus : endpoint_id
    device_endpoint_modbus ||--o{ device_modbus_reading : endpoint_id
    device_model_modbus_point ||--o{ device_modbus_reading : point_id
    devices ||--o{ device_modbus_reading : target_device_id

    device_endpoint_modbus {
        int endpoint_id PK_FK
        int unit_id
    }

    device_modbus_reading {
        int id PK
        int endpoint_id FK
        int point_id FK
        int unit_id
        int address
        int target_device_id FK
        varchar point_name
        tinyint enabled
    }
```

---

## 3. 등록 API

### 3.1 등록 — `POST .../modbus/readings`

**구현 상태:** ✅

#### 요청

```json
{
  "pointId": 2,
  "unitId": 1,
  "address": 11415,
  "targetDeviceId": 18,
  "pointName": "TOTAL_WT",
  "enabled": true
}
```

| 필드 | 필수 | 설명 |
|------|------|------|
| `pointId` | O | 원본 장비 모델의 point. `requiresInstance=true`여야 함 |
| `unitId` | O | 0~247 |
| `address` | O | 보정이 끝난 실제 주소. 0~65535 |
| `targetDeviceId` | O | 결과를 기록할 장비 |
| `pointName` | O | 결과 필드명 |
| `enabled` | X | 생략 시 true |

#### 응답 — `200 OK`

```json
{
  "success": true,
  "data": {
    "id": 5,
    "endpointId": 17,
    "sourceDeviceId": 17,
    "pointId": 2,
    "unitId": 1,
    "address": 11415,
    "targetDeviceId": 18,
    "pointName": "TOTAL_WT",
    "enabled": true
  }
}
```

| 조건 | HTTP | 메시지(예) |
|------|------|------------|
| device 없음 | 404 | `Device not found: {deviceId}` |
| endpoint 없음 / 다른 장비 소속 | 404 | `DeviceProtocolEndpoint not found: {endpointId}` |
| endpoint가 Modbus 아님 | 400 | `endpoint protocol must be modbus` |
| Modbus 설정 없음 | 404 | `DeviceEndpointModbus not found for endpoint: {endpointId}` |
| point가 원본 모델 소속 아님 | 404 | `DeviceModelModbusPoint not found for source model: {pointId}` |
| point가 `requiresInstance=false` | 400 | `point must require instance mapping` |
| target 장비 없음 | 404 | `Device not found: {targetDeviceId}` |
| (target, pointName) 중복 | 409 | `Modbus reading already exists for target device and point name` |
| 주소 범위 초과 | 400 | `address range exceeds 65535` |

---

## 4. 수정 API

### 4.1 수정 — `PUT .../modbus/readings/{readingId}`

**구현 상태:** ✅

전체 교체. 소속 endpoint를 제외한 모든 필드(`pointId`, `targetDeviceId` 포함)를 바꿀 수 있습니다.

요청 body는 `DeviceModbusReadingUpdateRequest` — 등록과 필드는 같지만 **`enabled`가 필수**입니다. 전체 교체 시 생략된 `enabled`가 기본값 true로 덮여 꺼둔 회선이 켜지는 것을 막기 위함입니다.

| 조건 | HTTP | 메시지(예) |
|------|------|------------|
| reading 없음 / 다른 endpoint 소속 | 404 | `DeviceModbusReading not found: {readingId}` |
| `enabled` 생략 | 400 | `enabled is required` |
| (그 외) | | 등록 API와 동일 — 새 point·target 기준으로 재검증, 중복 검사는 자기 자신 제외 |

---

## 5. 조회 API

### 5.1 목록 — `GET .../modbus/readings`

**구현 상태:** ✅

- ID 오름차순. 비활성 항목 포함
- Modbus 설정은 있지만 reading이 없으면 `200`, `data: []`

### 5.2 단건 — `GET .../modbus/readings/{readingId}`

**구현 상태:** ✅

| 조건 | HTTP |
|------|------|
| 존재 | 200 |
| 없음 / 다른 endpoint 소속 | 404 |

조회도 등록·수정과 같은 endpoint 검증(프로토콜·Modbus 설정)을 거칩니다.

---

## 6. 삭제 API

### 6.1 삭제 — `DELETE .../modbus/readings/{readingId}`

**구현 상태:** ✅

| 조건 | HTTP | 동작 |
|------|------|------|
| 존재 | 200 | 삭제, `readingId` 반환 |
| 없음 | 404 | |
| endpoint가 Modbus 아님 | 400 | 다른 API와 동일 규칙 |

부모(`device_endpoint_modbus`, `device_protocol_endpoint`)와 참조 대상(point, target 장비)은 삭제하지 않습니다.

---

## 7. API 요약

| Method | Path | 설명 | 상태 |
|--------|------|------|------|
| `GET` | `.../modbus/readings` | 목록 | ✅ |
| `GET` | `.../modbus/readings/{readingId}` | 단건 | ✅ |
| `POST` | `.../modbus/readings` | 등록 | ✅ |
| `PUT` | `.../modbus/readings/{readingId}` | 수정 | ✅ |
| `DELETE` | `.../modbus/readings/{readingId}` | 삭제 | ✅ |

일괄 등록(`/bulk`)은 없습니다. UI 요구가 생기면 SNMP point의 `/bulk` 패턴을 따릅니다.

---

## 8. 데이터 예시 — IRCACCURA

### 카탈로그 (모델당 1번)

`device_model_modbus_point` — ACCURA 모델

| id | name | register_type | data_type | byte_order | address | requires_instance | scale |
|----|------|---------------|-----------|------------|---------|-------------------|-------|
| 2 | POWER | HOLDING | FLOAT32 | CDAB | NULL | **1** | 1000 |

### 인스턴스

`devices`: IRCACCURA(17), CHILLER(18), IRCCOOLER(19)

`device_protocol_endpoint`: id 17 → device 17, `14.42.43.207:30500`

`device_endpoint_modbus`: endpoint 17, `unit_id = NULL` (회선별)

`device_modbus_reading`

| id | endpoint_id | point_id | unit_id | address | target_device_id | point_name |
|----|-------------|----------|---------|---------|------------------|------------|
| 4 | 17 | 2 | 0 | 11265 | 17 (자신) | TOTAL_WT |
| 5 | 17 | 2 | 1 | 11415 | 18 (CHILLER) | TOTAL_WT |
| 6 | 17 | 2 | 2 | 11565 | 19 (IRCCOOLER) | TOTAL_WT |
| 7 | 17 | 2 | 3 | 11715 | 17 | WC-FAN-TOTAL_WT |
| 8 | 17 | 2 | 7 | 12265 | 17 | LIQ-LOAD-TOTAL_WT |
| 9 | 17 | 2 | 8 | 12415 | 17 | IMC-TOTAL_WT1 |
| 10 | 17 | 2 | 9 | 12565 | 17 | IMC-TOTAL_WT2 |

### 수집 결과 (Influx)

```
device_id=17 (IRCACCURA) : TOTAL_WT, WC-FAN-TOTAL_WT, LIQ-LOAD-TOTAL_WT, IMC-TOTAL_WT1, IMC-TOTAL_WT2
device_id=18 (CHILLER)   : TOTAL_WT      ← 자기 endpoint(34196)의 온도와 합쳐짐
device_id=19 (IRCCOOLER) : TOTAL_WT
```

한 장비(CHILLER)의 필드가 **서로 다른 endpoint 두 곳**에서 올 수 있습니다 — 자기 endpoint에서 온도, IRCACCURA endpoint에서 전력.

### RDC는 reading 없음

RDC 같은 단순 장비는 카탈로그에 주소가 고정(`requires_instance=0`)되어 있어 `device_endpoint_modbus.unit_id`만으로 수집됩니다. reading을 등록하려 하면 `point must require instance mapping`으로 거부됩니다.

---

## 9. 알려진 제약

| 항목 | 내용 |
|------|------|
| 수집 미연동 | `CollectionGroupSpecService`가 `scriptType=modbus`를 아직 지원하지 않아 reading을 등록해도 수집 스크립트가 생성되지 않음 → [BACKLOG](../BACKLOG.md) 1.8 |
| endpoint 프로토콜 변경 | endpoint의 프로토콜을 modbus에서 다른 것으로 바꾸면 그 밑 Modbus 설정·reading은 남지만 잠김(모든 API 400). 수집기는 프로토콜로 걸러 무시. 정리는 `DELETE .../endpoints/{ep}/modbus`로 통째로 |
| 같은 레지스터 중복 | `(endpoint, unit, address)` UK는 없음 — 같은 값을 여러 목적지로 보낼 수 있게 의도. 수집 시 중복 읽기 최적화는 1.8에서 |
| UI 없음 | Ops Console에 회선 매핑 화면 없음 → [BACKLOG](../BACKLOG.md) 1.9 |

---

## 10. 구현 현황

| 구분 | 내용 |
|------|------|
| DDL | `43_device_modbus_reading.sql` (신규) · `44_alter_modbus_reading_endpoint.sql` (기존 DB) ✅ |
| 도메인 | `DeviceModbusReading` — unit/address 범위, point.requiresInstance, 레지스터 범위 검증 ✅ |
| Repository | 도메인 인터페이스 + JPA/SpringData ✅ |
| Application | `DeviceModbusReadingQueryService` — CRUD 5개, `validateReadingSource` 공통 검증 ✅ |
| API | 목록·단건·등록·수정·삭제 ✅ |
| 테스트 | `DeviceModbusReadingTest` (엔티티) · `DeviceModbusReadingRequestTest` (DTO) · `DeviceModbusReadingGetTest` (MockMvc) · `DeviceModbusReadingCascadeTest` (H2로 실제 DDL CASCADE) · `DeviceModbusReadingRepositoryPathTest` (쿼리 메서드 경로) — 22개 ✅ |

---

## 11. 갱신 이력

| 날짜 | 변경 |
|------|------|
| 2026-09-16 | 조회 API 문서 최초 작성 |
| 2026-09-20 | CRUD 전체·테이블·FK 정책·계층 구조·데이터 예시로 확장. 스키마 번호 27→43 |
