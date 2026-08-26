# Device Capabilities API 설계

`device` 모듈의 **수집·UI용 capabilities 조회** API입니다. 장비·모델 point·endpoint·SNMP instance를 JOIN하여 **resolved OID**를 반환합니다.

> API prefix: `GET /api/manager/devices/capabilities`  
> 아키텍처: [DEVICE_ARCHITECTURE.md](./DEVICE_ARCHITECTURE.md) §5.2  
> 샘플 데이터: [demo_capabilities_devices.sql](../../sql/samples/demo_capabilities_devices.sql)

---

## 1. 개요

| 개념 | 설명 |
|------|------|
| **Device** | 현장 장비 인스턴스 |
| **page_widget_device** | 페이지(위젯)↔장비. `pageCode` 필터 |
| **DeviceModelSnmpPoint** | 모델 OID 템플릿/고정 OID |
| **DeviceProtocolEndpoint** | SNMP host/port |
| **DeviceSnmpInstance** | `{instanceId}` 치환값 |

구 manager의 `codeKey` / zone ID 하드코딩 대신, **pageCode + location**으로 장비를 고른 뒤 point·OID를 합성합니다.

### 1.1 이번 범위

| 포함 | 미포함 |
|------|--------|
| SNMP point OID 합성 | Modbus capabilities |
| pageCode · location 필터 | Telemetry 조회 |
| includeSubtree | pointCategory / displayRole |
| enabled 장비·point만 | SRC `device_snmp_point` |

### 1.2 OID 치환 규칙

| `requiresInstance` | `device_snmp_instance` | `resolvedOid` |
|--------------------|------------------------|---------------|
| `false` | (무관) | `oid` 그대로 |
| `true` | 있음 | `{instanceId}` → instance 값 치환 |
| `true` | 없음 | `null` (point는 포함) |

---

## 2. 조회 API

### 2.1 목록 — `GET /api/manager/devices/capabilities`

**구현 상태:** ✅

#### 쿼리 파라미터

| 파라미터 | 필수 | 설명 |
|----------|------|------|
| `pageCode` | N | `DEVICE_PAGE` code (예: `ENVIRONMENT`) |
| `locationNodeCode` | N | 위치 code |
| `includeSubtree` | N | `true`이면 `locationNodeCode` 하위 노드 포함 |

- `enabled=true` 장비만
- `enabled=true` SNMP point만
- `enabled=true` SNMP endpoint만 사용
- 정렬: `device.id` 오름차순

#### 응답 — `200 OK`

```json
{
  "success": true,
  "data": [
    {
      "deviceId": 101,
      "deviceName": "PDU-데모-좌",
      "locationNodeName": "Demo-Rack-01",
      "modelId": 1,
      "modelName": "AP8959-DEMO",
      "manufacturer": "APC",
      "endpoint": {
        "endpointId": 201,
        "host": "192.168.1.10",
        "port": 161,
        "instanceId": 1
      },
      "points": [
        {
          "pointId": 1,
          "name": "V",
          "unit": "V",
          "oidTemplate": "1.3.6.1.4.1.318.1.1.26.8.3.3.1.2.1.{instanceId}.3",
          "resolvedOid": "1.3.6.1.4.1.318.1.1.26.8.3.3.1.2.1.1.3",
          "requiresInstance": true
        }
      ]
    }
  ]
}
```

`endpoint`가 없으면 `null`. SNMP 프로토콜이 없는 모델은 `points: []`.

#### 오류

| 조건 | HTTP | 메시지(예) |
|------|------|------------|
| location 없음 | 404 | `LocationNode not found: {code}` |

매칭 장비 없음 → `200` + 빈 배열.

---

## 3. 샘플 데이터

V001~V014 적용 후 아래 SQL을 실행하면 데모 데이터가 들어갑니다.

```bash
mysql -h HOST -P PORT -u dcim -p dcim_new < sql/samples/demo_capabilities_devices.sql
```

확인 예:

```http
GET /api/manager/devices/capabilities?pageCode=ENVIRONMENT&locationNodeCode=DEMOZONE01&includeSubtree=true
```

---

## 4. 구현 상태

| 항목 | 상태 |
|------|------|
| API | ✅ |
| 통합 테스트 | ✅ |
| 샘플 SQL | ✅ |
