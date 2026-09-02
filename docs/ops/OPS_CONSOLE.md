# Ops Console

경로: `http://localhost:8080/ops-console.html`  
정적 운영 등록 UI. 업체 본 UI 아님. **계산(PUE 등)은 서버 aggregate API** (`GET /api/manager/query/aggregate`).

## 수집 장비 추가 흐름 (UI가 쓰는 API)

```text
1. (선택) POST /location-node
2. GET  /device-models  → modelId, protocols[].id
3. POST /devices  (+ pathCodeId: PDU Path 피드)
4. POST /devices/{id}/endpoints          { protocolTypeId, host, port }
5. POST .../endpoints/{id}/snmp-instance { instanceId }
6. POST 또는 PUT /collector/tasks/.../groups  deviceIds 에 장비 추가
7. (표시) POST /widgets  deviceIds + pointNames
8. GET  /query/last?widgetId=
```

## 탭 · API 커버리지

| 탭 | 연결 API | 상태 |
|----|----------|------|
| 한눈에 보기 | 부트 시 목록 집계 | ✅ |
| 장비 추가 | Device + endpoint + instance + task group attach (+ Path) | ✅ |
| 장비 목록 | Device CRUD(Path 포함), endpoint CRUD, instance, task attach | ✅ |
| 수집 작업 | Task CRUD/toggle, group POST/DELETE/toggle | ✅ |
| 화면 위젯 | Widget CRUD, last/count/chart | ✅ (layout → widget-dashboard) |
| 위치 | location-node CRUD, parent DnD (Path는 장비에) | ✅ (bulk 미연결) |
| 장비 모델 | Model CRUD, SNMP point CRUD | ✅ (Modbus create는 API 예정으로 생략) |
| 공통코드 | code-groups CRUD(조회·생성), common-codes CRUD | ✅ |
| API 안내 | 커버리지 표 | – |

### 위치
| API | UI |
|-----|-----|
| GET/POST location-node | 탐색기 · 추가 |
| PUT /{code} | 이름·유형 |
| PATCH /{code}/parent | 드래그앤드롭 |
| DELETE /{code}, /subtree | 삭제 |
| POST /bulk | 미연결 |
| GET /devices?locationNodeCode=&includeSubtree= | 선택 위치 장비 |

### 장비 Path
| API | UI |
|-----|-----|
| POST/PUT `/devices` `pathCodeId` | 장비 추가 마법사 · 장비 수정 (`LOCATION_PATH`) |
| 응답 `pathCode` / `pathName` | 목록 chip · 상세 |

### 외부 / 미구현
- Live: `live-test.html`
- Query aggregate (usage / power / pue): `GET /api/manager/query/aggregate` 구현됨. Ops Console 위젯에서 preset·기간·(pue) IT 장비 선택 후 집계 조회 가능.
- Device capabilities: 미연결
- Modbus point 쓰기: API 예정

## 응답 형태

공통: `{ status, message, error, timestamp, data }`

- devices 목록: `data.content[]`, `page`는 **1부터** (+ `pathCodeId` / `pathCode` / `pathName`)
- location 목록: 트리 `children[]`
- capability: `GET /devices/capabilities?pageCode=&locationNodeCode=`

## 관련 화면

| URL | 용도 |
|-----|------|
| `/` | 도구 인덱스 |
| `/ops-console.html` | **등록·운영 UI** (코드·위치·모델·장비·Task·위젯·Query) |
| `/widget-dashboard.html` | 위젯 배치 · last · count · chart(Chart.js) |
| `/live-test.html` | 실시간 MQTT |
