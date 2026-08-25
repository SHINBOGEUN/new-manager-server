# SQL 스키마 이력

DB 스키마 변경은 **코드와 같이 Git으로 관리**합니다.  
JPA `ddl-auto: none`(운영/개발)이므로, 테이블 생성·변경은 이 폴더의 SQL을 기준으로 적용합니다.

---

## 폴더 구조

```
sql/
├── README.md                          ← 이 문서
└── history/
    ├── V001__create_users_table.sql   ← users
    ├── V002__create_code_group_table.sql
    ├── V003__create_common_code_table.sql
    └── V00N__...(추후).sql           ← 변경 시 버전 추가
```

---

## 파일命名 규칙

```
V{번호}__{설명}.sql
```

| 예시 | 용도 |
|------|------|
| `V001__create_users_table.sql` | 테이블 최초 생성 |
| `V002__add_index_users_refresh_token.sql` | 인덱스 추가 |
| `V003__alter_users_role_not_null.sql` | 컬럼 변경 |

- **번호는 순차 증가** (이미 적용된 번호는 수정하지 않음)
- **이미 운영에 적용된 파일은 내용을 바꾸지 않고**, 변경이 필요하면 **새 V00N 파일** 추가
- 설명은 영문 snake_case 권장 (파일명 호환)

---

## 적용 순서

1. `history/`에서 번호 순으로 파일 확인
2. 아직 적용 안 한 버전부터 DB에 실행
3. `docs/ERD.md` 스키마·관계도 갱신

```bash
# MariaDB 예시 (test 프로파일 기준)
mysql -h 192.168.10.14 -P 20181 -u dcim -p dcim_new < sql/history/V001__create_users_table.sql
```

---

## 적용 이력 (수동 기록)

| 버전 | 파일 | 적용일 | 환경 | 비고 |
|------|------|--------|------|------|
| V001 | `V001__create_users_table.sql` | | dcim_new | users 최초 생성 |
| V002 | `V002__create_code_group_table.sql` | | dcim_new | code_group 생성 |
| V003 | `V003__create_common_code_table.sql` | | dcim_new | common_code 생성 (V002 선행) |
| V004 | `V004__create_location_node_table.sql` | | dcim_new | location_node + LOCATION_TYPE·UNASSIGNED 시드 (V003 선행) |
| V005 | `V005__create_device_model_tables.sql` | | dcim_new | device_model(+device_type_id), device_model_protocol, DEVICE_TYPE·PROTOCOL_TYPE 시드 |
| V006 | `V006__create_device_model_snmp_point.sql` | | dcim_new | device_model_snmp_point (V005 선행) |
| V007 | `V007__create_devices_table.sql` | | dcim_new | devices (V004·V005 선행, location NOT NULL → UNASSIGNED) |
| V008 | `V008__add_device_model_device_type_id.sql` | | dcim_new | **기존 DB용** device_type_id ALTER·백필(id 2→13, 3→3)·FK |
| V018 | `V018__create_page_widget.sql` | | dcim_new | page_widget + point 1:N (V003·V014 선행) |

> 운영 반영 후 위 표에 날짜·환경을 채워 주세요.

---

## 관련 문서

- [ERD.md](../docs/ERD.md) — 테이블·컬럼 다이어그램
