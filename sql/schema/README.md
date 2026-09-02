# 신규 DB 스키마 (baseline)

운영 DB(`dcim_new`) **현재 구조**를 FK 생성 순서대로 나눈 DDL입니다.  
**빈 DB 신규 설치**에는 `history/V001~V021` 대신 이 폴더를 권장합니다.

## 적용 순서

`01` → `22` 번호 순으로 실행합니다.

```bash
for f in sql/schema/[0-9][0-9]_*.sql; do
  mysql -h HOST -P PORT -u dcim -p dcim_new < "$f"
done
mysql -h HOST -P PORT -u dcim -p dcim_new < sql/seed/REQUIRED_BOOTSTRAP.sql
```

Windows PowerShell 예:

```powershell
Get-ChildItem sql/schema/*_*.sql | Sort-Object Name | ForEach-Object {
  Get-Content $_.FullName -Raw -Encoding UTF8 | mysql -h HOST -P PORT -u dcim -pdcim_new
}
```

## `history/` 와의 관계

| 방식 | 용도 |
|------|------|
| `sql/schema/01~22` | **신규** 빈 DB — V021 반영 최종 스키마 한 번에 |
| `sql/history/V001~V021` | **기존** DB 증분 마이그레이션 (이미 적용된 번호는 재실행 금지) |

### `history` 체인 검토 요약 (2026-09-02)

- **V001~V021 빈 DB 순차 적용:** 대체로 가능 (V013 번호만 공백, V008은 V005와 `device_type_id` 중복)
- **재실행:** V012, V015, V020, V021은 비-idempotent → **실패**
- **시드:** V004/V005/V014/V017/V019에 분산 → 신규는 `REQUIRED_BOOTSTRAP.sql` 1회 권장

기존 운영 DB는 **미적용분만** `history`에서 추가 실행하세요 (예: V020, V021).

## 테이블 목록

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
| 16 | `16_page_widget_aggregate.sql` | page_widget_aggregate (usage/power/pue) |
| 17 | `17_page_widget_count.sql` | page_widget_count |
| 18 | `18_page_widget_chart.sql` | page_widget_chart |
| 19 | `19_page_widget_point.sql` | page_widget_point |
| 20 | `20_page_widget_device.sql` | page_widget_device (+ device_role) |
| 21 | `21_page_widget_model.sql` | page_widget_model |
| 22 | `22_page_widget_layout.sql` | page_widget_layout |

스냅샷 출처: `192.168.10.14:20181` / `dcim_new` (2026-09-02).
