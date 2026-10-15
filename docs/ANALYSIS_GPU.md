# GPU 분석

기존 분석 조회를 확장한다. 새 DB 테이블이나 Sensor Data 변경은 필요하지 않다.

## 설정과 화면

1. 장비 페이지 설정에서 GPU 서버를 ANALYSIS에 등록한다.
2. 모델의 ANALYSIS 노출 설정에서 DEVICE/COMPONENT 포인트를 허용한다.
3. 분석 화면에서 장비를 선택하면 서버 전체 항목과 GPU별 트리를 함께 표시한다.
4. 선택 기간의 GPU 목록을 자동 조회하며 GPU 0, GPU 1 등 각 가지의 측정항목을 독립적으로 선택한다. GPU 목록 새로고침도 가능하다.
5. 조회 결과는 기존 카테고리·단위별 그래프에 표시하며 범례에 GPU 번호를 포함한다.

기간 변경 시 인덱스 목록과 선택을 초기화하고 재조회한다. 제거된 GPU라도 선택 기간에 데이터가 있으면 목록에 나온다.
새로 추가된 인덱스는 데이터가 있는 시점부터 표시한다.

## API

POST `/api/manager/analysis/measurements`의 target에 선택적인 `componentIndices` 배열을 추가한다.
생략/null이면 전체, 빈 배열이면 컴포넌트 미선택이다. DEVICE 포인트에는 영향을 주지 않는다.
트리 UI는 `componentSelections: [{pointName, componentType, componentIndex}]`를 사용하여 각 GPU의 포인트를 개별 지정한다. 빈 배열이면 컴포넌트는 조회 결과에서 제외하고 DEVICE 선택은 유지한다.

```json
{
  "startDate": "2026-10-01",
  "endDate": "2026-10-15",
  "targets": [{"deviceId": 33, "pointNames": ["power", "gpu_temperature"], "componentIndices": ["0"]}]
}
```

POST `/api/manager/analysis/components`에 동일 요청 형태를 전달하면 해당 기간에 존재하는
deviceId/componentType/componentIndex 목록을 반환한다. 발견은 공통 통계 쿼리(1d)로 수행하여 전체 raw 응답을 전송하지 않는다.
두 API 모두 ANALYSIS 등록 장비 및 DB 노출 포인트를 검증하며 요청 경로/필드를 그대로 쿼리에 사용하지 않는다.

분석 Field 응답에 componentType/componentIndex를 추가한다. 기존 pointName은 유지하고 displayName은 GPU 번호를 포함한다.
일반 장비의 두 필드는 null이며 기존 요청과 생성자를 유지한다.

## 집계

- 7일 이하 raw, 31일 이하 30m, 92일 이하 3h, 183일 이하 12h, 이후 1d (기존 정책).
- device_id/point_name/component_type/component_index로 그룹한다.
- UUID, MAC, 위치 등 변동 태그는 그룹에서 제외하여 같은 논리 장비·인덱스의 교체 전후를 이어 조회한다.
- GPU마다 별도 Influx 쿼리를 실행하지 않는다. 장비/포인트 묶음으로 공통 조회한다.
- 응답은 min/max/avg 또는 raw이며 카테고리와 단위는 DB 설정을 사용한다.
- 수집 중단 구간의 자동 gap 판정은 전송 주기 정책이 필요하므로 이번 변경에서 추가하지 않는다.

실제 Influx 조회와 그래프는 실행 중인 Manager에서 추가 확인한다.
