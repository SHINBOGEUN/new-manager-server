# new-manager-server 개발 백로그

다음 개발 대기열입니다. **구 manager-server 문서(00~21)는 기능 의도만 참고**하고, API·패키지·하드코딩을 그대로 옮기지 않습니다.

> 기준: `main` @ `6a3f05b` (PR #10 merge)  
> 갱신: 2026-08-14 — **페이지 배정 = device 단위** (point 단위 아님)  
> 브랜치: `feat/device-page`  
> 노션: [device 후속 이슈 백로그](https://app.notion.com/p/3b5ee0a8ebbc81f8aeaae1af0e907a46)  
> 설계 원칙: [DEVICE_ARCHITECTURE.md](./device/DEVICE_ARCHITECTURE.md) §6

---

## 0. 현재 베이스 (구현됨)

| 영역 | DDL | API | 문서 |
|------|-----|-----|------|
| identity | V001 | login/register/refresh/validate | — |
| common_code | V002, V003 | code-group / common-code | — |
| location_node | V004 | 트리 CRUD | [LOCATION_NODE_API.md](./location/LOCATION_NODE_API.md) |
| device_model + protocols | V005, V008 | CRUD | [DEVICE_MODEL_API.md](./devicemodel/DEVICE_MODEL_API.md) |
| model snmp point | V006, **V012 OID UK** | CRUD | [DEVICE_MODEL_SNMP_POINT_API.md](./devicemodel/DEVICE_MODEL_SNMP_POINT_API.md) |
| model modbus point | V010 | CRUD | [DEVICE_MODEL_MODBUS_POINT_API.md](./devicemodel/DEVICE_MODEL_MODBUS_POINT_API.md) |
| devices | V007 | CRUD | [DEVICE_API.md](./device/DEVICE_API.md) |
| device_protocol_endpoint | V009 | CRUD | [DEVICE_ENDPOINT_API.md](./device/DEVICE_ENDPOINT_API.md) |
| device_snmp_instance | **V011** | **CRUD 완료** | [DEVICE_SNMP_INSTANCE_API.md](./device/DEVICE_SNMP_INSTANCE_API.md) |

**의도적 보류:** SNMP community/version은 DB에 두지 않음 (앱 기본값). SRC `device_snmp_point`는 나중에.

**DDL 적용 확인 (운영/테스트 DB):** V011 → V012 순서. V012 전에 동일 protocol 중복 OID가 있으면 UK 실패.

---

## 개발 원칙 (모든 다음 작업)

- 벤더·사이트 분기 금지 (`IRC`/`CPC`, Accura 레지스터 상수, `dragino-sensor` switch)
- showroom / coupang **전용 Controller path 금지**
- OID·레지스터·Influx field는 **테이블(카탈로그)** 에만
- **어느 페이지에 보일지 = device 단위 매핑** (point에 페이지 붙이지 않음)
- 구 MD의 URL/패키지를 복제하지 않음 — 기능 의도만 흡수 (구 DashboardGroup / analysisYn / codeKey 하드코딩 대체)

---

## 다음 착수 — Device ↔ Page (이 브랜치)

구 manager: Environment/Cooling/Analysis가 `codeKey`·zone ID·`analysisYn`으로 장비를 고름.  
**대체:** 장비마다 노출 페이지를 DB에 등록.

```text
device A → ENVIRONMENT, ANALYSIS
device B → COOLING, ANALYSIS
device C → POWER
```

| ID | 항목 | 내용 |
|----|------|------|
| D1 | 페이지 코드 | `common_code` 그룹 예: `DEVICE_PAGE` — ENVIRONMENT, COOLING, ANALYSIS, POWER … (페이지 추가 = 코드 행 추가) | ✅ |
| D2 | 매핑 테이블 | ~~`device_page`~~ → `page_widget_device` (V018에서 DROP) | ✅ |
| D3 | API | ~~`/devices/{id}/pages`~~ → `/widgets` deviceIds | ✅ |
| D4 | 목록 필터 | `GET /devices?pageCode=` — 위젯 연결 장비 | ✅ |
| D5 | DDL·문서·테스트 | V018 page_widget 일괄, PAGE_WIDGET_API | ✅ |

**하지 않음 (이 브랜치):** point↔page, SRC snmp_point, capabilities/telemetry 구현.

참고: 구 `dashboard_group_device_mapping`과 같은 **장비 단위 매핑** 패턴. 그룹 전용이 아니라 페이지 코드로 일반화.

---

## Phase 1 — 수집 메타 (Device-Page 이후)

| ID | 항목 | 우선 | 비고 |
|----|------|------|------|
| 1.1 | 모델 point 성격 메타 (선택) | 중 | 측정값 성격(온습도/전력) — **페이지 배정과 별개**. 나중에 capabilities용 |
| 1.2 | `device_snmp_point` (SRC형) | 나중에 | 사용자 결정: SRC 보류 |
| 1.3 | Device Modbus 확장 | 중 | unit_id / slave |
| 1.4 | Device 모델 변경 vs endpoint 정합성 | 중 | ✅ 409 거부 |
| 1.5 | host 형식 검증 | 낮 | |
| 1.6 | Device nested `endpoints[]` | 낮 | |

### SRC 합의 (보류)

| 유형 | 예 | OID |
|------|-----|-----|
| A. 템플릿형 | PDU | 모델 OID 패턴 + `device_snmp_instance` |
| B. 장비정의형 | SRC, door, 배연창 | **`device_snmp_point`에 전체 OID** — 나중에 |

---

## Phase 2 — Capabilities + Collector

| ID | 항목 | 비고 |
|----|------|------|
| 2.1 | `GET /devices/capabilities` | **pageCode + location** 등으로 장비 먼저 고르고, point/OID 해석 | ✅ |
| 2.2 | 수집 스크립트 export + collector 연동 | |
| 2.3 | 범용 probe API | 구 pduTest |

---

## Phase 3 — Telemetry

| ID | 항목 | 비고 |
|----|------|------|
| 3.1 | Influx 태그 규약 | |
| 3.2 | `/telemetry/last`, `/telemetry/chart` | |
| 3.3 | MQTT 실시간 | 어댑터만 |

---

## Phase 4 — 조회 UI용 API

Environment / Cooling / Analysis / Dashboard는 **페이지 위젯에 묶인 장비** → capabilities / last·chart·aggregate.

| ID | 항목 |
|----|------|
| 4.1 | Analysis |
| 4.2 | Environment / Dashboard 조회 (last ✅ / chart / aggregate) — [QUERY_LAST_API.md](./query/QUERY_LAST_API.md) |
| 4.2w | 페이지 위젯 `page_widget` — DDL V018 + CRUD ✅, [PAGE_WIDGET_API.md](./device/PAGE_WIDGET_API.md) |
| 4.3 | DashboardGroup (페이지 매핑과 역할 정리 — 중복이면 page로 흡수) |
| 4.4 | Cooling / GPU |

---

## Phase 5 — 운영 부가

| ID | 구 문서 | 항목 |
|----|---------|------|
| 5.1 | 01_alert | Alert history |
| 5.2 | 14_setting | Power threshold |
| 5.3 | 13_report | Report schedule |
| 5.4 | 18_slack | Slack |
| 5.5 | 10_file | File (필요 시) |
| 5.6 | 04_auth | Auth0/Okta |

---

## Phase 6 — Asset / Space / 계층

| ID | 항목 |
|----|------|
| 6.1 | Rack U / formFactor / 엑셀 |
| 6.2 | Site layout |
| 6.3 | `devices.parent_device_id` |
| 6.4 | Display alias |

---

## 다음 한 줄

**완료(이 브랜치):** 페이지 위젯(CRUD + device + 2D layout), `GET /query/last?widgetId=`, `device_page` 제거(V018).
**완료 추가:** `GET /query/count` — 전체 enabled 장비 대상 + `countMode`(total/by_model/model). DDL V019(`count_mode`, `count_model_id`). 규칙은 [PAGE_WIDGET_API.md](./device/PAGE_WIDGET_API.md).
**다음:** aggregate(일반, PUE는 후순위) → chart. (3D layout는 추후)
**참고:** last/aggregate 장비 범위 = `page_widget_device`. count는 `devices.enabled=1` 전체.
