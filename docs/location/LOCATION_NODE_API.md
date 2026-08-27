# LocationNode API 설계

`location` 모듈의 위치 트리 노드(`location_node`) API·비즈니스 규칙을 정리한 문서입니다.

> API prefix: `/api/manager/location-node`  
> 관련 ERD: [ERD.md — location_node](../ERD.md#location_node--위치-트리-노드-location-모듈)  
> DDL: [V004__create_location_node_table.sql](../../sql/history/V004__create_location_node_table.sql)

---

## 1. 개요

> Path(전원 피드 A/B/C)는 **위치 노드가 아니라 장비(`devices.path_code_id`)** 에 둡니다. 차트 `by_path`는 [QUERY_CHART_API](../query/QUERY_CHART_API.md) · [DEVICE_API](../device/DEVICE_API.md) 참고.

위치 노드는 **트리 구조**로 관리합니다. 루트가 여러 개인 **포레스트**도 허용합니다.

| 개념 | 설명 |
|------|------|
| 루트 노드 | `parent_code = null` |
| 자식 노드 | `parent_code`로 부모 노드에 연결 |
| 위치 유형 | `common_code` 중 `group_key = 'LOCATION_TYPE'`만 허용 |
| 리프 노드 | 자식이 없는 노드 |
| `code` | **PK**. 일반 노드: 서버가 **10자 Base62** 랜덤 생성. **시스템 시드: `UNASSIGNED`**. 불변. API·FK 식별자 |

트리 구조의 진실 원천은 **`parent_code`** 입니다.  
사용자는 **`name`** 만 입력합니다.

### 1.1 공통 제약

| 항목 | 규칙 |
|------|------|
| `name` | 필수. 같은 부모 아래에서 중복 불가 (`uk_location_node_parent_code_name`, 루트는 앱 검증) |
| `code` | 서버 자동 생성 (10자 Base62, `[0-9A-Za-z]`). 사용자 입력 없음. 변경 불가. **예외: `UNASSIGNED` 시드** |
| `location_type_id` | 필수. `LOCATION_TYPE` 그룹 소속만 허용 |
| `UNASSIGNED` | 삭제·이름 변경·부모 변경 금지 (시스템 루트) |
| 순환 참조 | 금지 (자기 자신·자손을 부모로 지정 불가) |

### 1.2 devices 연동 · 미배정(`UNASSIGNED`)

장비(`devices`)는 위치 노드의 **`code`를 FK**(`CHAR(10)`, **NOT NULL**)로 참조합니다.

- CONTAINER에 슬라이딩 도어·배연창 → `location_node_code` = CONTAINER의 `code`
- ROW/RACK 장비도 동일 — **유형 제한 없음**
- **위치를 아직 모를 때** → `location_node_code = 'UNASSIGNED'` (V004 시드). 이후 수정 API로 실제 위치 지정

**V004 시드**

| 테이블 | 내용 |
|--------|------|
| `code_group` | `LOCATION_TYPE` |
| `common_code` | `UNASSIGNED`, `CONTAINER`, `ZONE`, `ROW`, `RACK` |
| `location_node` | `code=UNASSIGNED`, `name=미배정`, `parent_code=NULL` |

> API·DDL: [DEVICE_API.md](../device/DEVICE_API.md), [V007__create_devices_table.sql](../../sql/history/V007__create_devices_table.sql)

---

### 1.3 위치 유형(LOCATION_TYPE) 순서

`common_code.sort_order`로 **유형 간 계층 순서**를 판단합니다. (**구현됨**)

예시:

| code | name | sort_order (유형 순서) |
|------|------|------------------------|
| UNASSIGNED | 미배정 | -1 (시스템, 계층 순서 대상 아님) |
| CONTAINER | 컨테이너 | 0 |
| ZONE | 존 | 1 |
| ROW | 열 | 2 |
| RACK | 랙 | 3 |

유형 순서는 트리에서 **부모 → 자식 방향으로 단조 증가**해야 합니다.

---

## 2. 등록 API

### 2.1 단건 등록 — `POST /api/manager/location-node`

**구현 상태:** ✅ 구현됨

#### 요청

```json
{
  "parentCode": null,
  "locationTypeId": 1,
  "name": "컨테이너 A"
}
```

| 필드 | 필수 | 설명 |
|------|------|------|
| `parentCode` | 조건부 | `null`이면 **루트** 등록. 값이 있으면 해당 `code`를 부모로 **자식** 등록 |
| `locationTypeId` | O | `LOCATION_TYPE` common_code ID |
| `name` | O | 노드 표시명 |

#### 응답

서버가 생성한 `code`를 포함합니다.

```json
{
  "code": "K7mN2pQx9L",
  "parentCode": null,
  "locationTypeId": 1,
  "name": "컨테이너 A",
  "children": []
}
```

#### 자식 등록 시 트리 재구성 (핵심 규칙)

**구현 상태:** ✅ 구현됨

등록 전:

```
CONTAINER
└── ROW
```

`CONTAINER` 아래에 `ZONE` 유형 노드를 추가하면:

```
CONTAINER
└── ZONE
    └── ROW    ← 기존 ROW의 parent_code가 ZONE으로 변경
```

1. 새 노드 `N`을 부모 `P` 아래에 등록한다.
2. `P`의 **직접 자식** 중, `location_type` 순서가 `N`보다 **큰** 노드들을 찾는다.
3. 해당 노드들의 `parent_code`를 `N.code`로 변경한다.

#### 오류

| 조건 | HTTP | 메시지(예) |
|------|------|------------|
| 부모 없음 | 404 | `LocationNode not found: {parentCode}` |
| 위치 유형 없음 | 404 | `CommonCode not found: {locationTypeId}` |
| LOCATION_TYPE 아님 | 400 | `locationType must belong to LOCATION_TYPE group` |
| 형제 name 중복 | 400 | `name already exists under parent` |
| DB UK 위반 (동시 요청 등) | 400 | `name already exists under parent` |
| 유형 순서 위반 | 400 | `child location type must be deeper than parent` |

---

### 2.2 일괄 등록 — `POST /api/manager/location-node/bulk`

**구현 상태:** ✅ 구현됨

트리 구조로 요청하면 **부모 → 자식** 순으로 등록합니다. 단일 트랜잭션으로 처리됩니다.

#### 요청

```json
{
  "parentCode": null,
  "nodes": [
    {
      "locationTypeId": 1,
      "name": "컨테이너 A",
      "children": [
        {
          "locationTypeId": 2,
          "name": "ZONE 1",
          "children": [
            {
              "locationTypeId": 3,
              "name": "A열",
              "children": []
            }
          ]
        }
      ]
    }
  ]
}
```

| 필드 | 필수 | 설명 |
|------|------|------|
| `parentCode` | X | 기존 노드 아래에 붙일 부모 `code`. `null`이면 `nodes`가 루트로 등록 |
| `nodes` | O | 등록할 트리 목록 (포레스트 가능) |
| `nodes[].locationTypeId` | O | 위치 유형 ID |
| `nodes[].name` | O | 노드 이름 |
| `nodes[].children` | X | 하위 노드 (재귀) |

#### 응답

등록된 트리와 동일한 구조로 `code`가 채워져 반환됩니다.

```json
{
  "success": true,
  "data": [
    {
      "code": "K7mN2pQx9L",
      "parentCode": null,
      "locationTypeId": 1,
      "name": "컨테이너 A",
      "children": [
        {
          "code": "A1b2C3d4E5",
          "parentCode": "K7mN2pQx9L",
          "locationTypeId": 2,
          "name": "ZONE 1",
          "children": []
        }
      ]
    }
  ]
}
```

#### 규칙

- 같은 부모 아래 `name` 중복 불가 (요청 내·DB 모두 검증)
- 중간 노드 등록 실패 시 **전체 롤백**
- `parentCode`가 있으면 `nodes`의 각 항목이 해당 부모의 **직접 자식**으로 등록

---

## 3. 수정 API

### 3.1 기본 수정 — `PUT /api/manager/location-node/{code}`

**구현 상태:** ✅ 구현됨

`locationType`, `name` 수정. **`code`·`parentCode`는 변경 불가.**

#### 요청

```json
{
  "locationTypeId": 2,
  "name": "컨테이너 A (수정)"
}
```

| 필드 | 수정 |
|------|------|
| `locationType` | O |
| `name` | O |
| `code` | X (PK, 불변) |
| `parent` | X (별도 API) |

---

### 3.2 부모 변경 — `PATCH /api/manager/location-node/{code}/parent`

**구현 상태:** ✅ 구현됨

```json
{
  "parentCode": "A1b2C3d4E5"
}
```

| 필드 | 설명 |
|------|------|
| `parentCode` | 새 부모 `code`. `null`이면 루트 승격 |

---

### 3.3 일괄 수정 — 검토 중

**구현 상태:** ⬜ 미구현 · 필요 여부 미확정

---

## 4. 조회 API

### 4.1 트리 조회 — `GET /api/manager/location-node`

**구현 상태:** ✅ 구현됨

전체 노드를 조회한 뒤 **`children`에 중첩된 트리**로 반환합니다. 탐색기(폴더 트리) 형태입니다.

| 파라미터 | 설명 |
|----------|------|
| `name` | 이름 **부분 일치** 검색 (대소문자 무시). 매칭 노드의 **조상·자손 경로**를 포함 |
| `parentCode` | 해당 노드를 루트로 한 **서브트리** (노드 + 모든 자손). 없는 code면 404 |
| `locationTypeId` | 위치 유형 필터. 매칭 노드의 **조상·자손 경로**를 포함. 없는 ID면 404 |

파라미터 미지정 시 **전체 포레스트**(루트 노드 배열)를 반환합니다. 각 레벨은 `name` 오름차순입니다.

#### 응답 예시

```json
{
  "success": true,
  "data": [
    {
      "code": "TSTCNTR001",
      "parentCode": null,
      "locationTypeId": 1,
      "name": "테스트 컨테이너 01",
      "children": [
        {
          "code": "TSTZONE001",
          "parentCode": "TSTCNTR001",
          "locationTypeId": 2,
          "name": "테스트 존 01",
          "children": []
        }
      ]
    }
  ]
}
```

#### 필터 조합

| 조합 | 동작 |
|------|------|
| 없음 | 전체 트리 |
| `parentCode`만 | 해당 노드 + 하위 전체 |
| `name` / `locationTypeId` | 조건에 맞는 노드 + 경로(조상·자손) |
| `parentCode` + `name` / `locationTypeId` | 서브트리 안에서 필터. 루트는 항상 포함, 매칭 없으면 `[]` |

---

## 5. 삭제 API

### 5.1 단건 삭제 (리프만) — `DELETE /api/manager/location-node/{code}`

**구현 상태:** ✅ 구현됨

- 자식 없음 → 삭제 성공
- 해당 location을 참조하는 **device가 있으면 `UNASSIGNED`로 이동** 후 삭제
- 자식 있음 → 400 (`cannot delete node with children`)
- **`code = UNASSIGNED` → 409** (시스템 노드 삭제 금지)
- `UNASSIGNED`에서 이름 충돌 → 409 (`device name conflict at UNASSIGNED; rename devices before deleting location`)
- 노드 없음 → 404

#### 응답 — `200 OK`

```json
{
  "success": true,
  "data": {
    "deletedCode": "M4n3B2v1C0",
    "reassignedDeviceCount": 2
  }
}
```

### 5.2 서브트리 전체 삭제 — `DELETE /api/manager/location-node/{code}/subtree`

**구현 상태:** ✅ 구현됨

- 해당 노드 + 모든 자손 cascade 삭제 (깊은 노드부터)
- subtree 내 location을 참조하는 **device는 `UNASSIGNED`로 이동** 후 삭제
- `UNASSIGNED` 삭제 시도 → 409
- 이름 충돌 시 → 409 (5.1과 동일)
- 노드 없음 → 404

---

## 6. API 요약

| Method | Path | 설명 | 상태 |
|--------|------|------|------|
| `POST` | `/api/manager/location-node` | 단건 등록 | ✅ |
| `POST` | `/api/manager/location-node/bulk` | 트리 일괄 등록 | ✅ |
| `PUT` | `/api/manager/location-node/{code}` | 메타 수정 | ✅ |
| `PATCH` | `/api/manager/location-node/{code}/parent` | 부모 변경 | ✅ |
| `GET` | `/api/manager/location-node` | 트리 조회 | ✅ |
| `DELETE` | `/api/manager/location-node/{code}` | 리프만 삭제 | ✅ |
| `DELETE` | `/api/manager/location-node/{code}/subtree` | 서브트리 전체 삭제 | ✅ |

---

## 7. 구현 현황

| 구분 | 내용 |
|------|------|
| 도메인 | `createRoot`, `createChild`, `update`, `updateParent` |
| DTO | `LocationNodeCreateRequest`, `LocationNodeBulkCreateRequest`, `LocationNodeTreeCreateRequest`, `LocationNodeUpdateRequest`, `LocationNodeParentUpdateRequest` |
| 미구현 | 일괄 수정 (필요 여부 미확정) |

---

## 8. 갱신 이력

| 날짜 | 변경 |
|------|------|
| 2026-07-02 | 최초 작성 |
| 2026-07-02 | `depth` 제거, 하위 순차 삭제 API 제거 |
| 2026-07-02 | `id` 제거, `code` PK·서버 자동 생성, `parentCode` 기준으로 전환 |
| 2026-07-02 | `code` 형식을 UUID → **10자 Base62** 로 변경 (`LocationNodeCodeGenerator`) |
| 2026-07-02 | GET 응답을 트리(`children`) 구조로 정리 |
| 2026-07-22 | location 삭제 시 참조 device를 UNASSIGNED로 자동 이동 |
