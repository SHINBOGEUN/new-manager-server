# DeviceModel API 설계

`devicemodel` 모듈의 **장비 제품 모델**(`device_model`) API·비즈니스 규칙을 정리한 문서입니다.

> API prefix: `/api/manager/device-models`  
> 관련 ERD: [ERD.md — device_model](../ERD.md#device_model--장비-제품-모델-devicemodel-모듈)  
> DDL: [`05_device_model.sql`](../../sql/schema/05_device_model.sql), [`06_device_model_protocol.sql`](../../sql/schema/06_device_model_protocol.sql)

---

## 1. 개요

| 개념 | 설명 |
|------|------|
| **DeviceModel** | 제조사·제품명·**장비 유형** 등 **장비 SKU/제품군** 메타데이터 |
| **DeviceModelProtocol** | 모델 ↔ `PROTOCOL_TYPE` common_code **N:M** 조인 (`model_id`, `protocol_type_id`) |
| **Device** | 실제 장비 인스턴스. `model_id` FK로 모델 참조 (유형은 모델에 귀속) |

> 장비 인스턴스 CRUD: [DEVICE_API.md](../device/DEVICE_API.md)

장비 유형은 `common_code`(`MODEL_TYPE`)이며 `device_model.device_type_id`로 참조합니다.  
프로토콜 타입(SNMP, MQTT …)은 `common_code`(`PROTOCOL_TYPE`)에 존재하며, 모델과의 연결은 `device_model_protocol` 조인 테이블로 표현합니다.  
프로토콜 연결은 **DeviceModel 등록·수정 API**의 `protocols[]`로만 관리합니다 (별도 Protocol API 없음).

> SNMP 수집 point(OID) 정의: [DEVICE_MODEL_SNMP_POINT_API.md](DEVICE_MODEL_SNMP_POINT_API.md)

### 1.1 공통 제약

| 항목 | 규칙 |
|------|------|
| `name` | 필수 |
| `manufacturer` | 필수. `(name, manufacturer)` UK |
| `deviceTypeId` | 필수. `MODEL_TYPE` common_code만 허용 |
| `description` | 선택 |
| `protocols` | 등록·수정 시 **1개 이상** 필수 |
| `protocolTypeId` | `PROTOCOL_TYPE` common_code만 허용 |
| `(model_id, protocol_type_id)` | UK — 동일 모델에 같은 프로토콜 타입 중복 불가 |
| 수정 시 protocols | **전체 교체** (부분 수정 없음) |
| 모델 삭제 | `devices.model_id` 참조 중이면 409 |

---

## 2. 등록 API

### 2.1 등록 — `POST /api/manager/device-models`

**구현 상태:** ✅ 구현됨

#### 요청

```json
{
  "name": "LHT65N-PIR",
  "manufacturer": "Dragino",
  "deviceTypeId": 13,
  "description": "동작 감지 센서",
  "protocols": [
    { "protocolTypeId": 7 },
    { "protocolTypeId": 8 }
  ]
}
```

| 필드 | 필수 | 설명 |
|------|------|------|
| `name` | O | 모델/제품명 |
| `manufacturer` | O | 제조사 |
| `deviceTypeId` | O | `MODEL_TYPE` common_code ID |
| `description` | X | 설명 |
| `protocols` | O | 1개 이상 |
| `protocols[].protocolTypeId` | O | `PROTOCOL_TYPE` common_code ID |

#### 응답 — `201 Created`

```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "LHT65N-PIR",
    "manufacturer": "Dragino",
    "deviceTypeId": 13,
    "deviceTypeCode": "SENSOR",
    "deviceTypeName": "Sensor",
    "description": "동작 감지 센서",
    "protocols": [
      {
        "id": 10,
        "protocolTypeId": 7,
        "protocolCode": "mqtt",
        "protocolName": "MQTT"
      },
      {
        "id": 11,
        "protocolTypeId": 8,
        "protocolCode": "modbus",
        "protocolName": "Modbus"
      }
    ]
  }
}
```

#### 오류

| 조건 | HTTP | 메시지(예) |
|------|------|------------|
| 필수값 누락 | 400 | validation message |
| `(name, manufacturer)` 중복 | 400 | `device model already exists` |
| deviceTypeId 없음 | 404 | `CommonCode not found: {id}` |
| MODEL_TYPE 아님 | 400 | `deviceType must belong to MODEL_TYPE group` |
| protocolTypeId 없음 | 404 | `CommonCode not found: {id}` |
| PROTOCOL_TYPE 아님 | 400 | `protocolType must belong to PROTOCOL_TYPE group` |
| protocols 내 type 중복 | 400 | `duplicate protocol type in request` |
| protocols 빈 배열 | 400 | `at least one protocol required` |

---

## 3. 수정 API

### 3.1 수정 — `PUT /api/manager/device-models/{id}`

**구현 상태:** ✅ 구현됨

모델 메타 + **protocols 전체 교체**.

#### 요청

등록과 동일한 body.

#### 응답 — `200 OK`

등록 응답과 동일 구조.

#### 오류

등록 오류 + 아래 추가:

| 조건 | HTTP | 메시지(예) |
|------|------|------------|
| 모델 없음 | 404 | `DeviceModel not found: {id}` |

---

## 4. 조회 API

### 4.1 목록 — `GET /api/manager/device-models`

**구현 상태:** ✅ 구현됨

전체 모델 + `protocols[]` 중첩 반환. `protocols`는 `id` 오름차순.

| 파라미터 | 설명 |
|----------|------|
| `name` | 모델명 **부분 일치** (대소문자 무시) |
| `manufacturer` | 제조사 **부분 일치** (대소문자 무시) |

파라미터 미지정 시 전체 목록. 정렬: `manufacturer` → `name` 오름차순.

#### 응답 — `200 OK`

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "LHT65N-PIR",
      "manufacturer": "Dragino",
      "deviceTypeId": 13,
      "deviceTypeCode": "SENSOR",
      "deviceTypeName": "Sensor",
      "description": "동작 감지 센서",
      "protocols": [
        {
          "id": 10,
          "protocolTypeId": 7,
          "protocolCode": "mqtt",
          "protocolName": "MQTT"
        }
      ]
    }
  ]
}
```

### 4.2 단건 — `GET /api/manager/device-models/{id}`

**구현 상태:** ✅ 구현됨

목록 항목 1건과 동일 구조. 없으면 404.

---

## 5. 삭제 API

### 5.1 삭제 — `DELETE /api/manager/device-models/{id}`

**구현 상태:** ✅ 구현됨

| 조건 | HTTP | 동작 |
|------|------|------|
| 존재, 참조 없음 | 200 | 모델 + protocols 삭제 |
| 존재, `devices` 참조 | 409 | `device model is referenced by devices` |
| 없음 | 404 | `DeviceModel not found: {id}` |

---

## 6. API 요약

| Method | Path | 설명 | 상태 |
|--------|------|------|------|
| `GET` | `/api/manager/device-models` | 목록 (+ 필터) | ✅ |
| `GET` | `/api/manager/device-models/{id}` | 단건 | ✅ |
| `POST` | `/api/manager/device-models` | 등록 | ✅ |
| `PUT` | `/api/manager/device-models/{id}` | 수정 (protocols 전체 교체) | ✅ |
| `DELETE` | `/api/manager/device-models/{id}` | 삭제 | ✅ |

---

## 7. 구현 현황

| 구분 | 내용 |
|------|------|
| 도메인 | `DeviceModel.create/update`, `replaceProtocols`, `getSortedProtocols` |
| Application | `DeviceModelQueryService` — CRUD + protocols 전체 교체 |
| API | `DeviceModelController` — 5 endpoints |
| 삭제 제약 | `devices` 참조 시 409 (`device model is referenced by devices`) |

---

## 8. 갱신 이력

| 날짜 | 변경 |
|------|------|
| 2026-07-03 | 최초 작성 |
| 2026-07-06 | V005 DDL·N:M protocols 상세화 |
| 2026-07-07 | `is_default`, `sort_order`, `config` 제거 — 조인 테이블 최소화 |
| 2026-07-07 | DEVICE_MODEL_PROTOCOL_API.md 삭제 (별도 Protocol API 없음) |
