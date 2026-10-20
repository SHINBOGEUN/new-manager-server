# SQL 스키마 관리

DB 스키마는 **코드와 같이 Git으로 관리**합니다.  
JPA `ddl-auto: none`(운영/개발)이므로, 테이블 생성·변경은 `schema/` 폴더의 SQL을 기준으로 적용합니다.

---

## 폴더 구조

```
sql/
├── README.md
├── migration/       ← 기존 DB 전용 이관·정리 SQL (수동 검토 후 적용)
└── schema/          ← 신규 설치용·기존 DB용 DDL (01부터 연속 번호)
    ├── 01_users.sql
    ├── …
    ├── 49_device_page_device.sql
    ├── 53_device_mqtt_identity.sql
    ├── 54_device_model_mqtt_point.sql
    ├── 55_mqtt_source.sql
    ├── 56_mqtt_source_status.sql
    ├── 57_threshold_rule.sql
    ├── 58_alarm_event.sql
    └── 59_threshold_alarm_event.sql
```

스키마 파일은 FK 의존성과 신규 설치 순서대로 `01`부터 연속 번호를 사용합니다. 파일을 제거하거나 합칠 때는 뒤 파일을 함께 재번호화하고, SQL 테스트·문서의 참조도 같은 변경에서 갱신합니다. 기존 DB에 이미 적용된 DDL은 파일명 변경만으로 재실행하지 않습니다.

---

## 신규 배포 (빈 DB)

1. **스키마** — 아래 제외 목록을 적용해 `schema/01`부터 최신 번호까지 순서대로 실행
2. **기준 카탈로그** — 빈 DB에서만 `seed/model_catalog.sql` 실행
3. **공통코드·위치** — Ops Console(`/ops-console.html`)에서 현장별 위치·`UNASSIGNED` 노드 등록
4. **로그인 계정** — Ops Console 또는 API로 `users` 생성
5. **현장 데이터** — 실제 장비·Endpoint·수집 작업·계산 지표·위젯·자산을 등록

단, `ALTER` 형식의 증분 파일은 기존 DB 업그레이드용입니다. 신규 빈 DB에는 해당 변경이 반영된 최신 테이블 DDL을 사용하며, 이미 컬럼이 포함된 상태에서 같은 `ALTER` 파일을 다시 실행하지 않습니다.

신규 설치 제외: `43`(기존 DB용 ALTER), `44`·`45`(이관 전 LoRa 테이블), `46`·`47`(공통화 전 소스 테이블), `51`(기존 DB용 ALTER).
`44`~`47`은 기존 DB 이관 및 SQL 회귀 검증용 과거 구조로 보존하며 신규 설치에는 적용하지 않습니다. 최종 MQTT 테이블은 `53`~`56`으로 생성합니다. 폐기 테이블 정리문은 `migration/20261020_drop_retired_lora_tables.sql`에 있으며 신규 설치나 일반 업그레이드에서 자동 실행하지 않습니다.

```bash
for f in sql/schema/[0-9][0-9]_*.sql; do
  case "$(basename "$f")" in 43_*|44_*|45_*|46_*|47_*|51_*) continue ;; esac
  mysql -h HOST -P PORT -u dcim -p dcim < "$f"
done
```

Windows PowerShell:

```powershell
Get-ChildItem sql/schema/*_*.sql | Where-Object { $_.Name -notmatch '^(43|44|45|46|47|51)_' } | Sort-Object Name | ForEach-Object {
  Get-Content $_.FullName -Raw -Encoding UTF8 | mysql -h HOST -P PORT -u dcim -p dcim
}
```

---

## 스키마 변경 (기존 DB)

1. 운영 DB와 자산 파일 저장 경로를 먼저 백업
2. 운영 DB에 아직 적용하지 않은 새 DDL 파일만 실행
3. 애플리케이션 재기동 후 수집 상태·Collector Job 동기화를 확인

기존 DB에는 새 증분 SQL만 적용합니다. 파일에 포함된 확인 조회에서 미처리 행이 없는지 확인한 뒤 다음 애플리케이션 배포를 진행합니다.

임계값/알람 기능 배포 시에는 `57_threshold_rule.sql`~`59_threshold_alarm_event.sql`을 순서대로 적용합니다. `common_code.threshold_supported`는 `DATA_POINT_TYPE`에서만 사용하며, 임계값에서 허용할 측정 유형을 DB에서 관리합니다. 임계값은 장비·포인트 조합당 한 행이며 프로토콜은 실제 Influx 조회 경로를 가리키기 위해 보관합니다. 포인트 유형과 단위는 장비 측정 카탈로그에서 파생하므로 `threshold_rule`에는 중복 저장하지 않습니다. 기존 DB는 `migration/20261020_add_threshold_supported_to_common_code.sql`을 적용한 뒤 해당 유형을 Ops Console에서 허용합니다. 기존 JSON 규칙 테이블이 있다면 백업 후 `migration/20261020_normalize_threshold_rules.sql`을 추가 적용해 이전 테이블을 보존 이름으로 변경하고, 새 DDL을 생성합니다. 이미 정규화 테이블을 생성한 DB에는 `migration/20261020_simplify_threshold_rule_columns.sql`을 적용해 중복 컬럼을 제거하고 컬럼 주석을 추가합니다. 과거 규칙과 이벤트는 `*_legacy_json` 테이블에 보존되며 새 임계값은 재등록해야 합니다. `alarm_event`는 공통 이벤트, `threshold_alarm_event`는 임계값 발생 시의 장비·포인트·측정값을 각각 컬럼으로 저장합니다. 임계값 알람은 사용자에게 항상 표시됩니다. 실제 DB 적용은 별도 운영 절차에서 수행합니다.

활성 테이블의 목적 주석이 비어 있는 기존 DB에는 `migration/20261020_document_active_tables.sql`을 적용합니다. 이 스크립트는 테이블 주석만 설정하며 데이터·컬럼 구조는 변경하지 않습니다. 신규 설치에서는 각 `schema/*.sql`의 테이블 및 컬럼 COMMENT를 사용합니다. 운영 DB에서 발견된 레거시 자산 컬럼은 `devices`에 보존되어 있으며 신규 자산 관리는 `device_asset`을 사용합니다. `lora_ingest_error_log`와 `*_legacy_json`은 과거 데이터/정리 대상이므로 신규 설치용 baseline에는 포함하지 않습니다.

```bash
mysql -h HOST -P PORT -u dcim -p dcim < sql/schema/41_device_asset_document.sql
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
| 36~38 | Rack 배치·이미지·배치 이력 | device_rack_placement / device_image |
| 39~41 | 자산 이력·상세·문서 | device_asset 계열 |
| 42 | `42_device_modbus_reading.sql` | device_modbus_reading (Modbus 회선 매핑) |
| 43 | `43_alter_modbus_reading_endpoint.sql` | 기존 DB용 ALTER — reading FK → device_endpoint_modbus |
| 44~45 | 과거 LoRa/Dragino 장비·포인트 | MQTT 통합 전 구조. 신규 설치 제외, 이관 회귀 검증용 |
| 46~47 | 과거 LoRa MQTT 수집 소스·상태 | 공통화 전 구조. 신규 설치 제외, 이관 회귀 검증용 |
| 48 | `48_device_modbus_bit_field.sql` | Modbus 원본 reading의 비트 구간별 파생 point |
| 49 | `49_device_page_device.sql` | DEVICE_PAGE에 직접 선택한 장비 |
| 50 | `50_device_page_model_point_setting.sql` | 페이지·모델별 포인트 표시 설정 |
| 51 | `51_add_model_point_category_code.sql` | 기존 DB용 ALTER — 모델 포인트 CATEGORY FK 추가 |
| 52 | `52_external_data.sql` | 외부 데이터 JSON 수신 이력 |
| 53 | `53_device_mqtt_identity.sql` | MQTT 외부 식별값과 devices 연결 (LoRa 포함) |
| 54 | `54_device_model_mqtt_point.sql` | 공통 MQTT 모델 포인트, DEVICE/COMPONENT 범위 |
| 55 | `55_mqtt_source.sql` | 공통 MQTT 브로커·토픽·메시지 형식 |
| 56 | `56_mqtt_source_status.sql` | 공통 MQTT 소스 연결·수신 상태 |
| 57 | `57_threshold_rule.sql` | 임계값 규칙 |
| 58 | `58_alarm_event.sql` | 통합 알람 이력 |
| 59 | `59_threshold_alarm_event.sql` | 발생 당시 임계값·측정값 상세 |

## LoRa → 공통 MQTT 이관 (2026-10-14)

기존 DB에는 `schema/53`, `schema/54`를 먼저 실행하지 않습니다. 현재 LoRa 테이블을 사용하는
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

신규 DB에서는 구 `schema/46`, `schema/47`을 건너뛰고 `schema/55`, `schema/56`을 사용합니다.
이관 SQL과 신규 생성 SQL을 같은 기존 DB에 혼용하지 않습니다. 자세한 API와 배포 순서는
`docs/MQTT_SOURCE_API.md`를 참고합니다. Sensor Data의 공통 소스 구독 전환은 별도 작업입니다.

## 적용 이력 (수동 기록)

| 일자 | 대상 DB | 적용 내용 |
|------|---------|-----------|
| 2026-09-02 | — | `schema/01~22` baseline 확정. 구 `history/`, `seed/`, `dumps/` 제거 |
| 2026-09-03 | — | `schema/23_device_endpoint_modbus.sql` 추가 (Modbus endpoint 확장, unit_id) |
| 2026-10-02 | `dcim` 개발 DB | 계산 지표 테이블 최종 구조로 전환. 기존 계산 지표 정의·위젯은 보존하고 구 PUE 그룹 연결 구조는 제거 |
