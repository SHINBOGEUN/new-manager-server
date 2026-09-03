# SQL 스키마 관리

DB 스키마는 **코드와 같이 Git으로 관리**합니다.  
JPA `ddl-auto: none`(운영/개발)이므로, 테이블 생성·변경은 `schema/` 폴더의 SQL을 기준으로 적용합니다.

---

## 폴더 구조

```
sql/
├── README.md
└── schema/          ← DDL (01~22 baseline, 이후 번호로 증분 추가)
    ├── 01_users.sql
    ├── …
    └── 22_page_widget_layout.sql
```

운영 DB(`dcim_new`) **현재 구조**를 FK 생성 순서대로 나눈 baseline입니다 (스냅샷: `192.168.10.14:20181`, 2026-09-02).

---

## 신규 배포 (빈 DB)

1. **스키마** — `schema/01` ~ `schema/22` 번호 순 실행
2. **공통코드·위치** — Ops Console(`/ops-console.html`)에서 필수 그룹/코드·`UNASSIGNED` 노드 등록
3. **로그인 계정** — Ops Console 또는 API로 `users` 생성
4. **현장 데이터** — 모델·장비·위젯 등 UI로 등록

```bash
for f in sql/schema/[0-9][0-9]_*.sql; do
  mysql -h HOST -P PORT -u dcim -p dcim_new < "$f"
done
```

Windows PowerShell:

```powershell
Get-ChildItem sql/schema/*_*.sql | Sort-Object Name | ForEach-Object {
  Get-Content $_.FullName -Raw -Encoding UTF8 | mysql -h HOST -P PORT -u dcim -pdcim_new
}
```

---

## 스키마 변경 (기존 DB)

1. `schema/`에 변경 반영
   - **신규 설치용:** 해당 테이블 DDL 파일 수정
   - **기존 DB용:** `23_alter_설명.sql`처럼 **다음 번호**로 ALTER 스크립트 추가
2. 운영 DB에 미적용분만 실행
3. 아래 **적용 이력**에 기록

```bash
mysql -h HOST -P PORT -u dcim -p dcim_new < sql/schema/23_alter_example.sql
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

---

## 적용 이력 (수동 기록)

| 일자 | 대상 DB | 적용 내용 |
|------|---------|-----------|
| 2026-09-02 | — | `schema/01~22` baseline 확정. 구 `history/`, `seed/`, `dumps/` 제거 |
