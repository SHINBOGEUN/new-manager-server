# Device Page API 설계

`device` 모듈의 **장비↔노출 페이지** 매핑 API입니다.

> API prefix: `/api/manager/devices/{deviceId}/pages`  
> 목록 필터: `GET /api/manager/devices?pageCode=ENVIRONMENT`  
> DDL: [V013__create_device_page.sql](../../sql/history/V013__create_device_page.sql)  
> 시드: [V014__seed_device_page_codes.sql](../../sql/history/V014__seed_device_page_codes.sql)  
> 백로그: [BACKLOG.md](../BACKLOG.md)

---

## 1. 개요

구 manager-server는 Environment / Cooling / Analysis 장비를 `codeKey`·zone ID·`analysisYn`으로 골랐습니다.  
**대체:** 장비마다 보일 페이지를 DB에 등록합니다. (**point 단위 아님**)

```text
device A → ENVIRONMENT, ANALYSIS
device B → COOLING, ANALYSIS
device C → POWER
```

| 개념 | 설명 |
|------|------|
| **DEVICE_PAGE** | `code_group.group_key`. 페이지 코드 그룹 |
| **page code** | `ENVIRONMENT`, `COOLING`, `ANALYSIS`, `POWER` … (common_code) |
| **device_page** | `(device_id, page_code_id)` N:M 매핑 |

페이지가 늘면 **common_code 행만 추가**하면 됩니다.

### 1.1 공통 제약

| 항목 | 규칙 |
|------|------|
| `deviceId` | 존재하는 `devices.id` |
| `pageCodeId` | `DEVICE_PAGE` 그룹 `common_code`만 |
| UK | `(device_id, page_code_id)` |
| device 삭제 | 매핑 **CASCADE** |

---

## 2. 테이블 — `device_page`

**구현 상태:** ✅

| 컬럼 | 타입 | NULL | 키 | 설명 |
|------|------|------|-----|------|
| `id` | INT | N | PK | 매핑 ID |
| `device_id` | INT | N | FK, UK* | `devices.id` |
| `page_code_id` | INT | N | FK, UK* | `common_code.id` |
| `created_dt` | TIMESTAMP(6) | Y | | |
| `updated_dt` | TIMESTAMP(6) | Y | | |

\* UK: `(device_id, page_code_id)`

---

## 3. API

### 3.1 목록 — `GET /api/manager/devices/{deviceId}/pages`

**구현 상태:** ✅

장비에 연결된 페이지 목록. `id` 오름차순.

### 3.2 등록 — `POST /api/manager/devices/{deviceId}/pages`

**구현 상태:** ✅

```json
{ "pageCodeId": 10 }
```

| 조건 | HTTP | 메시지 |
|------|------|--------|
| device 없음 | 404 | `Device not found: {id}` |
| pageCode 없음 | 404 | `CommonCode not found: {id}` |
| DEVICE_PAGE 그룹 아님 | 400 | `pageCode must belong to DEVICE_PAGE group` |
| 이미 연결됨 | 409 | `page already linked to this device` |

### 3.3 전체 교체 — `PUT /api/manager/devices/{deviceId}/pages`

**구현 상태:** ✅

```json
{ "pageCodeIds": [10, 11] }
```

기존 매핑을 지우고 요청 목록으로 교체. 빈 배열이면 전부 해제.

### 3.4 삭제 — `DELETE /api/manager/devices/{deviceId}/pages/{pageId}`

**구현 상태:** ✅

매핑 행 `id` 기준 삭제. 응답 `data` = 삭제된 `pageId`.

### 3.5 장비 목록 필터 — `GET /api/manager/devices?pageCode=ENVIRONMENT`

**구현 상태:** ✅

`pageCode` = common_code.`code` (예: `ENVIRONMENT`).  
해당 페이지에 연결된 장비만. 다른 필터(`modelId`, `name` …)와 AND.

---

## 4. 응답 DTO

```json
{
  "id": 1,
  "deviceId": 101,
  "pageCodeId": 10,
  "pageCode": "ENVIRONMENT",
  "pageName": "Environment"
}
```

---

## 5. API 요약

| Method | Path | 설명 | 상태 |
|--------|------|------|------|
| `GET` | `/devices/{deviceId}/pages` | 목록 | ✅ |
| `POST` | `/devices/{deviceId}/pages` | 단건 추가 | ✅ |
| `PUT` | `/devices/{deviceId}/pages` | 전체 교체 | ✅ |
| `DELETE` | `/devices/{deviceId}/pages/{pageId}` | 단건 삭제 | ✅ |
| `GET` | `/devices?pageCode=` | 페이지별 장비 필터 | ✅ |
