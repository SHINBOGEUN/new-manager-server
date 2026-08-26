# Ops Console 메모

경로: `http://localhost:8080/ops-console.html`  
정적 프로토타입. 업체 본 UI 아님. **계산(PUE 등)은 서버 aggregate API** 로 갈 예정.

## 수집 장비 추가 흐름 (UI가 쓰는 API)

```text
1. (선택) POST /location-node
2. GET  /device-models  → modelId, protocols[].id
3. POST /devices
4. POST /devices/{id}/endpoints          { protocolTypeId, host, port }
5. POST .../endpoints/{id}/snmp-instance { instanceId }
6. POST 또는 PUT /collector/tasks/.../groups  deviceIds 에 장비 추가
7. (표시) POST /widgets  deviceIds + pointNames
8. GET  /query/last?widgetId=
```

## Ops Console이 쓰는 API (전부 아님)

운영에 자주 쓰는 **조회·등록·위치 이동** 위주입니다. CRUD 전체·Live·Modbus·bulk는 빠져 있습니다.

### 위치 — 거의 완비 (+ 하위 장비)
| API | UI |
|-----|-----|
| GET/POST location-node | 탐색기 · 추가 |
| PUT /{code} | 이름·유형 |
| PATCH /{code}/parent | 드래그앤드롭 |
| DELETE /{code}, /subtree | 삭제 |
| POST /bulk | 미연결 |
| GET /devices?locationNodeCode=&includeSubtree= | 선택 위치의 장비 목록 |

### 그 외 대표 누락
- Device: PUT/DELETE, capabilities
- Model: CRUD, SNMP/Modbus point 쓰기
- Task: DELETE, group POST/DELETE/toggle
- Widget: PUT/DELETE/layout (layout는 widget-dashboard)
- Live: live-test.html
- Query aggregate/count/chart: **서버 API 자체 미구현**


## 응답 형태

공통: `{ status, message, error, timestamp, data }`

- devices 목록: `data.content[]`, `page`는 **1부터**
- location 목록: 트리 `children[]`
- capability: `GET /devices/capabilities?pageCode=&locationNodeCode=`

## 관련 화면

| URL | 용도 |
|-----|------|
| `/` | 도구 인덱스 |
| `/ops-console.html` | 메타·수집·위젯 CRUD |
| `/widget-dashboard.html` | 위젯 배치·last |
| `/live-test.html` | 실시간 MQTT |
