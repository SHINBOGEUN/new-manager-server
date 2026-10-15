# SQL 스키마 관리

DB 스키마는 **코드와 같이 Git으로 관리**합니다.  
JPA `ddl-auto: none`(운영/개발)이므로, 테이블 생성·변경은 `schema/` 폴더의 SQL을 기준으로 적용합니다.

---

## 폴더 구조

```
sql/
├── README.md
├── migration/       ← 기존 DB의 데이터 보존 이관 SQL
└── schema/          ← DDL (01~22 baseline, 이후 번호로 증분 추가)
    ├── 01_users.sql
    ├── …
    ├── 53_device_page_device.sql
    ├── 58_device_mqtt_identity.sql
    ├── 59_device_model_mqtt_point.sql
    ├── 60_mqtt_source.sql
    └── 61_mqtt_source_status.sql
```

운영 DB(`dcim_new`) **현재 구조**를 FK 생성 순서대로 나눈 baseline입니다 (스냅샷: `192.168.10.14:20181`, 2026-09-02).

---

## 신규 배포 (빈 DB)

1. **스키마** — 아래 제외 목록을 적용해 `schema/01`부터 최신 번호까지 순서대로 실행
2. **기준 카탈로그** — 빈 DB에서만 `seed/model_catalog.sql` 실행
3. **공통코드·위치** — Ops Console(`/ops-console.html`)에서 현장별 위치·`UNASSIGNED` 노드 등록
4. **로그인 계정** — Ops Console 또는 API로 `users` 생성
5. **현장 데이터** — 실제 장비·Endpoint·수집 작업·계산 지표·위젯·자산을 등록

단, `ALTER` 형식의 증분 파일은 기존 DB 업그레이드용입니다. 신규 빈 DB에는 해당 변경이 반영된 최신 테이블 DDL을 사용하며, 이미 컬럼이 포함된 상태에서 같은 `ALTER` 파일을 다시 실행하지 않습니다.

신규 설치 제외: `44`(기존 DB용 ALTER), `46`·`47`(이관 전 LoRa 테이블), `50`·`51`(공통화 전 소스 테이블), `56`(기존 DB용 ALTER).
`46`·`47`은 과거 구조 기록 및 기존 DB 이관 검증용으로 유지하며 최종 테이블은 `58`·`59`로 생성합니다.

```bash
for f in sql/schema/[0-9][0-9]_*.sql; do
  case "$(basename "$f")" in 44_*|46_*|47_*|50_*|51_*|56_*) continue ;; esac
  mysql -h HOST -P PORT -u dcim -p dcim < "$f"
done
```

Windows PowerShell:

```powershell
Get-ChildItem sql/schema/*_*.sql | Where-Object { $_.Name -notmatch '^(44|46|47|50|51|56)_' } | Sort-Object Name | ForEach-Object {
  Get-Content $_.FullName -Raw -Encoding UTF8 | mysql -h HOST -P PORT -u dcim -p dcim
}
```

---

## 스키마 변경 (기존 DB)

1. 운영 DB와 자산 파일 저장 경로를 먼저 백업
2. 운영 DB에 아직 적용하지 않은 새 DDL 파일만 실행
3. 애플리케이션 재기동 후 수집 상태·Collector Job 동기화를 확인

기존 DB에는 새 증분 SQL만 적용합니다. 파일에 포함된 확인 조회에서 미처리 행이 없는지 확인한 뒤 다음 애플리케이션 배포를 진행합니다.

```bash
mysql -h HOST -P PORT -u dcim -p dcim < sql/schema/42_device_asset_document.sql
```

---

## 필수 공통코드 (신규 설치 시)

| group_key | 예시 code | 없으면 |
|-----------|-----------|--------|
| `LOCATION_TYPE` | UNASSIGNED, CONTAINER, ZONE, ROW, RACK | 위치 트리 불가 |
| `LOCATION_PATH` | A, B, C | PDU Path / 차트 `by_path` 불가 |
| `MODEL_TYPE` | PDU, UPS, SENSOR, … | 모델 등록 불가 |
| `PROTOCOL_TYPE` | snmp, modbus, mqtt | endpoint·수집 Task 불가 |
| `DEVICE_PAGE` | ENVIRONMENT, COOLING, …, dashboard | 페이지 위젯 불가 |
| `DATA_POINT_TYPE` | POWER, ENERGY, TEMPERATURE, … | 모델 포인트 등록 불가 |
| `CATEGORY` | POWER, BATTERY, ETC, FLOW_PRESSURE, TEMPERATURE_HUMIDITY, STATUS | 측정 항목 분류 |
| `UNIT` | W, KWH, V, A, CELSIUS, PERCENT, … | SNMP·Modbus·MQTT(LoRa 포함) 포인트의 단위 참조 (단위 없는 포인트는 NULL) |
| `ASSET_STATUS` | ACTIVE, MAINTENANCE, FAULT, INACTIVE, RETIRED | 자산 상태 관리 불가 |
| `location_node` | UNASSIGNED | 장비 등록 FK 실패 |

---

## 테이블 목록 (baseline)

| # | 파일 | 테이블 |
|---|------|--------|
| 01 | `01_users.sql` | users |
| 02 | `02_code_group.sql` | code_group |
| 03 | `03_common_code.sql` | common_code |
| 04 | `04_location_node.sql` | location_node |
| 05 | `05_device_model.sql` | device_model |
| 06 | `06_device_model_protocol.sql` | device_model_protocol |
| 07 | `07_device_model_snmp_point.sql` | device_model_snmp_point |
| 08 | `08_device_model_modbus_point.sql` | device_model_modbus_point |
| 09 | `09_devices.sql` | devices |
| 10 | `10_device_protocol_endpoint.sql` | device_protocol_endpoint |
| 11 | `11_device_snmp_instance.sql` | device_snmp_instance |
| 12 | `12_collection_task.sql` | collection_task |
| 13 | `13_collection_task_group.sql` | collection_task_group |
| 14 | `14_collection_task_device.sql` | collection_task_device |
| 15 | `15_page_widget.sql` | page_widget |
| 16 | `16_page_widget_aggregate.sql` | page_widget_aggregate (usage/power) |
| 17 | `17_page_widget_count.sql` | page_widget_count |
| 18 | `18_page_widget_chart.sql` | page_widget_chart |
| 19 | `19_page_widget_point.sql` | page_widget_point |
| 20 | `20_page_widget_device.sql` | page_widget_device (+ device_role) |
| 21 | `21_page_widget_model.sql` | page_widget_model |
| 22 | `22_page_widget_layout.sql` | page_widget_layout |
| 23 | `23_device_endpoint_modbus.sql` | device_endpoint_modbus |
| 24~26 | 계산 지표 정의·소스·위젯 | calculated_metric 계열 |
| 27~29 | Psychrometric·last 소스 | page_widget_psychrometric / last_source |
| 30~32 | Power distribution | page_widget_power_distribution 계열 |
| 33~35 | 장비 그룹·위젯 연결 | device_group / widget |
| 37~39 | Rack 배치·이미지·배치 이력 | device_rack_placement / device_image |
| 40~42 | 자산 이력·상세·문서 | device_asset 계열 |
| 43 | `43_device_modbus_reading.sql` | device_modbus_reading (Modbus 회선 매핑) |
| 44 | `44_alter_modbus_reading_endpoint.sql` | (기존 DB용 ALTER) reading FK → device_endpoint_modbus |
| 46~49 | 과거 LoRa/Dragino 스키마 | 46·47은 MQTT 통합 전 구조. 신규 설치 제외. 48·49는 제거된 기능 정리 |
| 50~51 | LoRa MQTT 수집 소스·상태 | lora_mqtt_source / lora_mqtt_source_status |
| 52 | `52_device_modbus_bit_field.sql` | Modbus 원본 reading의 비트 구간별 파생 point |
| 53 | `53_device_page_device.sql` | DEVICE_PAGE에 직접 선택한 장비 |
| 54 | `54_device_page_model_point_setting.sql` | 페이지·모델별 포인트 표시 설정 |
| 55 | `55_device_model_modbus_point_data_point_type.sql` | Modbus 포인트에 DATA_POINT_TYPE FK 추가 (기존 DB용; 신규 설치는 08에 포함) |
| 56 | `56_add_model_point_category_code.sql` | SNMP·Modbus·LoRa 모델 포인트에 CATEGORY 코드 FK 추가 (기존 DB용; 신규 설치는 각 baseline에 포함) |
| 57 | `57_external_data.sql` | 외부 데이터 JSON 수신 이력 |
| 58 | `58_device_mqtt_identity.sql` | MQTT 외부 식별값과 devices 연결 (LoRa 포함) |
| 59 | `59_device_model_mqtt_point.sql` | 공통 MQTT 모델 포인트, DEVICE/COMPONENT 범위 |
| 60 | `60_mqtt_source.sql` | 공통 MQTT 브로커·토픽·메시지 형식 |
| 61 | `61_mqtt_source_status.sql` | 공통 MQTT 소스 연결·수신 상태 |

## LoRa → 공통 MQTT 이관 (2026-10-14)

기존 DB에는 `schema/58`, `schema/59`를 먼저 실행하지 않습니다. 현재 LoRa 테이블을 사용하는
Manager를 중지하고 백업한 후 `migration/20261014_unify_lora_mqtt.sql`을 한 번 실행하고 새 코드를 배포합니다.
이 스크립트는 `RENAME TABLE`과 `ALTER TABLE`로 두 기존 테이블을 전환합니다.
기존 PK·장비 ID·포인트 ID·단위·카테고리·생성 시각을 보존하므로
`device_page_model_point_setting`의 `point_id`와 Influx `device_id`를 다시 작성하지 않습니다.
DDL은 자동 커밋되므로 실패 시 적용된 단계부터 복구하며 스크립트 전체를 재실행하지 않습니다.

- `device_lora_endpoint` → `device_mqtt_identity`, `id_type` → `identity_type`.
- `device_model_lora_point` → `device_model_mqtt_point`. 기존 행은 `component_scope=DEVICE`, `component_path=''`.
- 기존 LoRa API URL과 JSON의 `idType`, `payloadField` 및 수집 소스 설정은 유지합니다.
- LoRa bulk/상태 API에는 LoRa 식별값·모델만 반환하며 GPU의 MAC과 COMPONENT 포인트는 제외합니다.
- 신규 MAC 등은 등록된 `devices.id`에 연결합니다. 같은 논리 장비의 MAC 변경은 같은 행에서 값을 갱신합니다.
- 새 물리 서버를 별도 장비로 관리할 때는 새 devices 행과 identity를 등록합니다.
- 페이지별 포인트 노출은 기존 `device_page_model_point_setting`을 사용합니다.
- GPU 개수·실제 UUID별 카탈로그 행을 생성하지 않습니다. 반복 구성요소 추출 경로만 정의합니다.

장비 식별값은 기존 등록·수정 API와 화면의 `mqttIdentities` 목록으로 관리합니다.
자세한 요청과 동작은 `docs/MQTT_DEVICE_REGISTRATION.md`를 참고합니다.
GPU 에이전트, Sensor Data 수신·Influx 저장, 구성요소별 분석 및 공통 포인트 관리/수집 조회 API는 후속 단계입니다.

---

## 공통 MQTT 수집 소스 이관

기존 DB는 `migration/20261014_unify_mqtt_sources.sql`로
`lora_mqtt_source`와 `lora_mqtt_source_status`를 `mqtt_source`와 `mqtt_source_status`로 전환합니다.
소스 ID·상태·브로커·토픽·자격증명 키·설정 버전을 보존합니다.
메시지 형식은 기존 `CHIRPSTACK_LORA`와 GPU/공통 에이전트용 `AGENT`를 지원합니다.

신규 DB에서는 구 `schema/50`, `schema/51`을 건너뛰고 `schema/60`, `schema/61`을 사용합니다.
이관 SQL과 신규 생성 SQL을 같은 기존 DB에 혼용하지 않습니다. 자세한 API와 배포 순서는
`docs/MQTT_SOURCE_API.md`를 참고합니다. Sensor Data의 공통 소스 구독 전환은 별도 작업입니다.

## 적용 이력 (수동 기록)

| 일자 | 대상 DB | 적용 내용 |
|------|---------|-----------|
| 2026-09-02 | — | `schema/01~22` baseline 확정. 구 `history/`, `seed/`, `dumps/` 제거 |
| 2026-09-03 | — | `schema/23_device_endpoint_modbus.sql` 추가 (Modbus endpoint 확장, unit_id) |
| 2026-10-02 | `dcim` 개발 DB | 계산 지표 테이블 최종 구조로 전환. 기존 계산 지표 정의·위젯은 보존하고 구 PUE 그룹 연결 구조는 제거 |
