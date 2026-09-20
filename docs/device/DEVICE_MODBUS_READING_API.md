# Modbus reading 조회 API

## 목록

`GET /api/manager/devices/{deviceId}/endpoints/{endpointId}/modbus/readings`

- Body 없음. `deviceId`는 수집 원본 장비 ID, `endpointId`는 그 장비의 공통 endpoint ID.
- ID 오름차순으로 해당 endpoint의 모든 reading을 반환. 비활성 항목도 포함.
- Modbus 설정은 존재하지만 reading이 없으면 `200`, `data: []`.

## 단건

`GET /api/manager/devices/{deviceId}/endpoints/{endpointId}/modbus/readings/{readingId}`

- Body 없음. `readingId`는 `device_modbus_reading.id`.
- 해당 endpoint 소속 reading만 조회. 없거나 다른 endpoint 소속이면 `404`.

## 공통 검증 및 응답

- 장비 없음, endpoint 없음 또는 다른 장비 소속, Modbus 설정 없음: `404`.
- endpoint 프로토콜이 Modbus가 아님: `400`.
- 대상 장비가 아닌 **수집 원본 장비** 기준 경로. `targetDeviceId`는 다른 장비일 수 있음.
- 기존 `ApiResponse` 사용. 목록의 `data`는 배열, 단건의 `data`는 객체.
- 각 항목: `id`, `endpointId`, `sourceDeviceId`, `pointId`, `unitId`, `address`, `targetDeviceId`, `pointName`, `enabled`.
- DB 스키마 변경 없음. 수집 실행이나 데이터 변경을 발생시키지 않음.

## 검증

`DeviceModbusReadingGetTest`: 실제 컨트롤러·서비스와 Mock 저장소를 사용해 목록/단건,
빈 목록, 비활성 항목, 소속 관계 및 400/404 응답을 검증. 실제 DB·보안 필터 통합 테스트는 별도.

## 이력

| 날짜 | 변경 |
|------|------|
| 2026-09-16 | reading 목록·단건 조회 추가 |
