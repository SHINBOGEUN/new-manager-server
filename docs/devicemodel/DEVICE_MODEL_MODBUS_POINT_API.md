# DeviceModel Modbus Point API 설계

`devicemodel` 모듈의 **모델별 Modbus 수집 point**(`device_model_modbus_point`) API·비즈니스 규칙을 정리한 문서입니다.

> API prefix: `/api/manager/device-models/{modelId}/protocols/{protocolId}/modbus-points`
> 부모 API: [DEVICE_MODEL_API.md](DEVICE_MODEL_API.md)
> 대응 문서: [DEVICE_MODEL_SNMP_POINT_API.md](DEVICE_MODEL_SNMP_POINT_API.md) — SNMP 버전(구조 동일)
> DDL: [`08_device_model_modbus_point.sql`](../../sql/schema/08_device_model_modbus_point.sql)

---

## 1. 개요

| 개념 | 설명 |
|------|------|
| **DeviceModel** | 장비 제품 모델 (SKU/제품군) |
| **DeviceModelProtocol** | 모델 ↔ `PROTOCOL_TYPE` 연결. Modbus는 `protocolCode = modbus` |
| **DeviceModelModbusPoint** | Modbus 프로토콜 연결 1건 아래의 **수집 point 정의** (레지스터 카탈로그) |
| **Device / endpoint** (향후) | 실제 장비. `host`, `port`, `unit_id`, 인스턴스 주소 등 **인스턴스별 값** 보유 |

SNMP와 동일하게 수집 정보는 두 층으로 나뉩니다.

| 층 | 저장 위치 | 예시 |
|----|-----------|------|
| **모델 카탈로그** | `device_model_modbus_point` | point별 register/data/byte order, `TOTAL_WT`, `W` |
| **장비 인스턴스** | `device_protocol_endpoint` + `device_endpoint_modbus` (향후) | `192.168.0.100`, `502`, `unit_id=3`, 인스턴스 주소 |

모델에는 **name + 읽기 방법(register_type/data_type/byte_order)** 을 미리 정의하고,
장비 등록·스크립트 생성 시 `host`, `port`, `unit_id`, 주소를 합쳐 최종 수집 명령을 만듭니다.

### 1.1 SNMP point와의 대응

Modbus point는 SNMP point와 **골격이 동일**하며 프로토콜 주소 체계만 다릅니다.

| SNMP point | Modbus point | 역할 |
|------------|--------------|------|
| `id`, `model_protocol_id`, `name`, `unit`, `enabled` | 동일 | 골격 |
| `oid` | `register_type` + `address` | 무엇을 어디서 읽나 |
| — | `data_type` + `byte_order` | raw 바이트를 어떻게 해석하나 (Modbus 특유) |
| — | `scale` | 원시값 배율 |
| `requires_instance` (OID `{instanceId}` 치환 필요) | `requires_instance` (주소를 인스턴스가 제공) | 인스턴스 값 필요 여부 |

### 1.2 주소 — 카탈로그 vs 인스턴스 (`requires_instance`)

같은 모델이라도 회로마다 주소가 다른 장비(분전반 등)가 있어, SNMP `{instanceId}`와 동일한 패턴으로 처리합니다.

```text
고정 주소:     requires_instance = 0 → address = 256   (쿨러 온도 레지스터 등, 모델 공통)
인스턴스 주소: requires_instance = 1 → address = NULL  (분전반 회로 등, 인스턴스가 주소 제공)
```

| 유형 | `requiresInstance` | `address` | 인스턴스 측 필요 값 |
|------|--------------------|-----------|---------------------|
| 고정 | `false` | 필수 (0~65535) | 없음 (unit_id만) |
| 인스턴스 | `true` | `NULL` | 주소 (endpoint_modbus) |

스크립트 생성 시:

```text
if point.requiresInstance:
  address = endpoint_modbus.address   // 회로별 실제 주소
else:
  address = point.address             // 모델 고정 주소
```

### 1.3 register_type / data_type / byte_order — 프로토콜 고정값 (enum)

세 컬럼 모두 Modbus 표준이 정한 유한 고정 집합이라 `common_code`가 아니라 **Java enum**으로 관리하고, DB에는 `@Enumerated(EnumType.STRING)`으로 **문자열 저장**합니다.

| enum | 값 | 보유 데이터 | 용도 |
|------|----|------------|------|
| `ModbusRegisterType` | COIL / DISCRETE / HOLDING / INPUT | `jsReadFunction` | 스크립트 생성 시 호출할 collector 함수명 (`readHoldingRegisters` 등) |
| `ModbusDataType` | INT16 / UINT16 / INT32 / UINT32 / FLOAT32 | `registerCount` | 읽을 레지스터 칸 수, 멀티 레지스터 여부 |
| `ModbusByteOrder` | ABCD / CDAB / BADC / DCBA | (라벨) | 멀티 레지스터 바이트 재배열 방식 |

> `byte_order`는 **멀티 레지스터**(INT32/UINT32/FLOAT32)일 때만 필수이며, 단일 레지스터(INT16/UINT16)이면 `NULL`이어야 합니다. 부호(signed/unsigned)는 별도 컬럼이 아니라 `data_type`(INT16 vs UINT16)으로 표현합니다.

### 1.4 `name` — 식별자이자 표시명

SNMP와 동일하게 `name`은 식별자이자 표시명입니다.

| 구분 | 예시 | 비고 |
|------|------|------|
| 분전반 전력 | `TOTAL_WT` | |
| 쿨러 온도 | `ONTO-TEMP`, `IN-TEMP` | |
| 다른 모델과 동일 name | 여러 모델의 `TOTAL_WT` | **다른 모델**이면 중복 허용 |

- **같은 모델·같은 Modbus protocol** 안에서만 `name` 유일 (UK)
- 모델 간 중복은 정상 (서로 다른 `model_protocol_id`)

### 1.5 boolean 컬럼 규칙 (프로젝트 공통)

[SNMP point 문서 1.3](DEVICE_MODEL_SNMP_POINT_API.md) 규칙을 그대로 따릅니다.

| 계층 | 규칙 | 예시 |
|------|------|------|
| DB | `TINYINT(1) NOT NULL DEFAULT 0` (또는 `1`) | `requires_instance`, `enabled` |
| Java | `boolean` + `@Column(nullable = false)` | `requiresInstance`, `enabled` |
| API JSON | `true` / `false` | `requiresInstance`, `enabled` |

### 1.6 공통 제약

| 항목 | 규칙 |
|------|------|
| `modelId` | 존재하는 `device_model.id` |
| `protocolId` | 해당 모델 소속 `device_model_protocol.id` |
| 프로토콜 타입 | **Modbus만** 허용 (`protocolCode = modbus`) |
| `name` | 필수. `(model_protocol_id, name)` UK |
| `registerType` / `dataType` | 필수 |
| `byteOrder` | 멀티 레지스터면 필수, 단일이면 `NULL` |
| `address` / `requiresInstance` | 상호 배타 (`requiresInstance=false` → address 필수 / `true` → address NULL) |
| `address` 범위 | 0 ~ 65535 |
| 목록 정렬 | `id` 오름차순 |
| point 삭제 | 건별 삭제 허용 |

---

## 2. 테이블 — `device_model_modbus_point`

**구현 상태:** ⬜ 엔티티 구현 / API 예정

| 컬럼 | 타입 | NULL | 키 | 기본값 | 설명 |
|------|------|------|-----|--------|------|
| `id` | INT | N | PK | AUTO_INCREMENT | point ID |
| `model_protocol_id` | INT | N | FK | | `device_model_protocol.id` |
| `name` | VARCHAR(255) | N | UK* | | 식별자·표시명 (`TOTAL_WT`, `ONTO-TEMP`) |
| `register_type` | VARCHAR(30) | N | | | `COIL`/`DISCRETE`/`HOLDING`/`INPUT` |
| `data_type` | VARCHAR(20) | N | | | `INT16`/`UINT16`/`INT32`/`UINT32`/`FLOAT32` |
| `byte_order` | VARCHAR(10) | Y | | | `ABCD`/`CDAB`/`BADC`/`DCBA` (멀티만) |
| `address` | INT | Y | | | 레지스터 주소 (고정 주소일 때) |
| `requires_instance` | TINYINT(1) | N | | `0` | 주소를 인스턴스가 제공하는지 (boolean) |
| `scale` | DOUBLE | Y | | | 원시값 배율 (NULL이면 1) |
| `unit` | VARCHAR(50) | Y | | | 단위 (`W`, `A`, `°C`, `%`) |
| `enabled` | TINYINT(1) | N | | `1` | 사용 여부 (boolean) |
| `created_dt` | TIMESTAMP(6) | Y | | `CURRENT_TIMESTAMP(6)` | 생성 시각 |
| `updated_dt` | TIMESTAMP(6) | Y | | `... ON UPDATE ...` | 수정 시각 |

\* UK: `(model_protocol_id, name)`

**FK 제약**

| FK | 참조 | ON DELETE | ON UPDATE |
|----|------|-----------|-----------|
| `fk_device_model_modbus_point_model_protocol_id` | `device_model_protocol(id)` | CASCADE | CASCADE |

**CHECK 제약**

| 제약 | 규칙 |
|------|------|
| `chk_..._requires_instance` | `requires_instance IN (0,1)` |
| `chk_..._enabled` | `enabled IN (0,1)` |
| `chk_..._address` | `requires_instance=1 AND address IS NULL` **또는** `requires_instance=0 AND address BETWEEN 0 AND 65535` |

---

## 3. API 요약 (예정)

| 메서드 | 경로 | 기능 |
|--------|------|------|
| POST | `/device-models/{modelId}/protocols/{protocolId}/modbus-points` | point 등록 |
| GET | `/.../modbus-points` | 목록 (id 오름차순) |
| GET | `/.../modbus-points/{pointId}` | 단건 |
| PUT | `/.../modbus-points/{pointId}` | 수정 (전체 교체) |
| DELETE | `/.../modbus-points/{pointId}` | 삭제 |

SNMP point API([DEVICE_MODEL_SNMP_POINT_API.md](DEVICE_MODEL_SNMP_POINT_API.md) §3~6)와 동일한 sub-resource 구조·오류 규칙을 따릅니다.

---

## 4. 예시 — 실제 장비 매핑

### 4.1 쿨러 (RDCOOLER) — 고정 주소, 다중 point

`host` 1개 + `unit_id` 1개 + point 여러 개(각 고정 주소). 단일 레지스터라 `byte_order`는 NULL.

| name | register_type | data_type | byte_order | address | requires_instance | scale | unit |
|------|---------------|-----------|------------|---------|-------------------|-------|------|
| ONTO-TEMP | INPUT | INT16 | NULL | 256 | 0 | 0.1 | °C |
| OFF-TEMP | INPUT | INT16 | NULL | 257 | 0 | 0.1 | °C |
| FAN_SPEED1 | INPUT | UINT16 | NULL | 4 | 0 | 0.1 | % |

> `signed:true` → `INT16`, `signed:false` → `UINT16`, `raw/10` → `scale=0.1`, `readInputRegisters` → `INPUT`.

### 4.2 쿨러 (IMCOOLER) — HOLDING 레지스터

| name | register_type | data_type | byte_order | address | requires_instance | scale | unit |
|------|---------------|-----------|------------|---------|-------------------|-------|------|
| IN-TEMP | HOLDING | INT16 | NULL | 8963 | 0 | 0.1 | °C |
| OUT-TEMP | HOLDING | INT16 | NULL | 8964 | 0 | 0.1 | °C |

### 4.3 분전반 회로 — 인스턴스 주소, FLOAT32

같은 측정(`TOTAL_WT`)을 회로(=별도 device)마다 다른 주소로 읽음 → 주소는 인스턴스가 제공.

| name | register_type | data_type | byte_order | address | requires_instance | scale | unit |
|------|---------------|-----------|------------|---------|-------------------|-------|------|
| TOTAL_WT | HOLDING | FLOAT32 | CDAB | NULL | 1 | 1000 | W |

> 실제 주소(11667 등)와 `unit_id`는 `device_endpoint_modbus`(향후)에 회로별로 저장. Influx는 회로마다 별도 `device_id` 태그.

---

## 5. 스크립트 생성 (향후)

manager-server가 카탈로그 + 인스턴스를 JOIN해 collector용 `collectData()` JS 스크립트를 생성합니다. collector(`C:\work\collector-service`)는 **수정 대상이 아니며**, GraalJS로 스크립트를 실행하고 아래 호스트 함수를 제공합니다.

```text
readCoils(host, port, unitId, address, quantity)            → Boolean[]
readDiscreteInputs(host, port, unitId, address, quantity)   → Boolean[]
readHoldingRegisters(host, port, unitId, address, quantity) → Integer[]  (raw uint16)
readInputRegisters(host, port, unitId, address, quantity)   → Integer[]  (raw uint16)
```

- 반환 규약: `collectData()`는 `{ deviceId: { fieldName: value } }` (value는 int/double)
- signed 변환·`byte_order` 재배열·`scale` 곱·반올림 등 **디코딩은 모두 생성된 JS 안에서** 수행 (collector는 raw uint16만 반환)
- 생성 매핑: `register_type` → 호출 함수(`jsReadFunction`), `data_type` → 읽을 칸 수·디코딩 스니펫, `byte_order` → 워드/바이트 재배열 스니펫, `scale` → 배율

---

## 6. device 모듈 연동 (향후)

| 값 | 저장 위치 | 비고 |
|----|-----------|------|
| `host`, `port` | `device_protocol_endpoint` (구현됨) | 프로토콜 공통 |
| `unit_id` | `device_endpoint_modbus` (예정) | Modbus slave ID |
| 인스턴스 주소 | `device_endpoint_modbus` (예정) | `requires_instance=1` point의 실제 주소 |

---

## 7. 구현 순서 (권장)

1. 도메인 엔티티 + enum — `DeviceModelModbusPoint`, `ModbusRegisterType`, `ModbusDataType`, `ModbusByteOrder` (완료)
2. DDL — [`08_device_model_modbus_point.sql`](../../sql/schema/08_device_model_modbus_point.sql) (완료)
3. Repository — `domain/repository` + `infrastructure/persistence`
4. Service — `application`
5. Controller + DTO — `api`, `api/dto`
6. `device_endpoint_modbus` (인스턴스 확장) — 별도 작업
7. 스크립트 생성기 — 별도 작업

---

## 8. 구현 현황

| 항목 | 상태 |
|------|------|
| 엔티티 · enum | 구현 완료 |
| V010 DDL | 구현 완료 |
| Repository / Service / Controller / DTO | 미구현 |
| device_endpoint_modbus | 미구현 |
| 스크립트 생성 | 미구현 |

---

## 9. 갱신 이력

| 날짜 | 변경 |
|------|------|
| 2026-07-24 | 문서 최초 작성, `DeviceModelModbusPoint` 엔티티·enum 및 V010 DDL 설계 |
