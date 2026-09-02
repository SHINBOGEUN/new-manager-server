# Query API (aggregate)

위젯에 묶인 **장비 + aggregate preset**으로 구간 집계값을 조회합니다.  
응답의 `value`는 **전체**, `devices[]`는 **장비별**입니다.

```http
GET /api/manager/query/aggregate?widgetId=12
GET /api/manager/query/aggregate?widgetId=12&rangePreset=this_month
```

---

## `GET /api/manager/query/aggregate`

| 이름 | 필수 | 설명 |
|------|------|------|
| `widgetId` | ✅ | `page_widget.id` (`queryKind=aggregate`) |
| `rangePreset` | | `last_24h` \| `today` \| `yesterday` \| `last_7d` \| `this_month` \| `last_month`. 미지정 시 위젯 `aggregateRangePreset`, 없으면 usage→`today` / power·pue→`last_24h` |

### 동작

1. 위젯 로드 (`queryKind=aggregate`, enabled)
2. `page_widget_device`의 **enabled** 장비 (역할별, 최대 200)
3. `page_widget_aggregate.op` preset에 따라 계산 (`page_widget_point`의 **측정 항목 1개** 사용)
4. 응답: `value`(전체, 소수 2자리) · `devices[]`(장비별, 소수 2자리) · `unit`(가능 시)

### preset별 계산

| aggregatePreset (`op`) | 계산 |
|------------------------|------|
| `usage` | 선택 포인트 구간 **첫값→끝값** 차분. `value` = 합. 음수(카운터 리셋) 스킵 |
| `power` | 선택 포인트 구간 **마지막값**. `value` = 합 |
| `pue` | `devices[].role=total|it` 각각 last. `value` = Σtotal / Σit |

위젯 생성 시 `pointNames`에 **정확히 1개** (예: `TOTAL_WT`, `TOTAL_KWH`, `W`). 모델마다 이름이 달라도 사용자가 선택합니다.

### 응답 예

```json
{
  "status": 200,
  "data": {
    "widgetId": 12,
    "widgetName": "오늘 사용량",
    "pageCode": "dashboard",
    "aggregatePreset": "usage",
    "rangePreset": "today",
    "start": "2026-08-31T00:00:00Z",
    "end": "2026-08-31T05:30:00Z",
    "value": 128.5,
    "unit": "kWh",
    "contributingDevices": 2,
    "devices": [
      { "deviceId": 19, "deviceName": "Bogeun-A-PDU", "role": "default", "value": 80.0 },
      { "deviceId": 20, "deviceName": "Bogeun-B-PDU", "role": "default", "value": 48.5 }
    ]
  }
}
```

데이터 없으면 `value`는 `null`, `devices`는 `[]`.

### 오류

| 조건 | HTTP |
|------|------|
| widgetId 없음 | 400 |
| 위젯 없음 | 404 |
| queryKind ≠ aggregate | 400 |
| 위젯 disabled | 400 |
| rangePreset 잘못됨 | 400 |
| 장비 > 200 | 400 |
| Influx 실패 | 503 |
