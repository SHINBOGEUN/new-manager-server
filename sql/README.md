# SQL 스키마 이력



DB 스키마 변경은 **코드와 같이 Git으로 관리**합니다.  

JPA `ddl-auto: none`(운영/개발)이므로, 테이블 생성·변경은 이 폴더의 SQL을 기준으로 적용합니다.



---



## 폴더 구조



```

sql/

├── README.md

├── history/                           ← 스키마 DDL (번호 순)

├── seed/

│   └── REQUIRED_BOOTSTRAP.sql         ← 신규 배포 필수 시드 (공통코드·UNASSIGNED)

└── samples/                           ← 데모·현장 샘플 (필수 아님)

    ├── demo_capabilities_devices.sql

    ├── demo_dashboard_widgets.sql

    └── demo_pdu_snmp_collection.sql

└── scripts/

    └── cleanup_test_data.sql          ← 테스트 DB 정리 (Influx 수집 장비만 유지)

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



- **번호는 순차 증가** (이미 적용된 번호는 수정하지 않음)

- **이미 운영에 적용된 파일은 내용을 바꾸지 않고**, 변경이 필요하면 **새 V00N 파일** 추가



---



## 신규 배포 순서



1. **스키마** — `history/` 번호 순 (V001 ~ 최신)

2. **필수 시드** — [`seed/REQUIRED_BOOTSTRAP.sql`](./seed/REQUIRED_BOOTSTRAP.sql)

3. **샘플(선택)** — `samples/demo_*.sql`

4. **로그인 계정** — `users`는 시드에 없음. 앱/API로 생성



```bash

mysql -h HOST -P PORT -u dcim -p dcim_new < sql/history/V001__create_users_table.sql

# … V002 ~ V018 …

mysql -h HOST -P PORT -u dcim -p dcim_new < sql/seed/REQUIRED_BOOTSTRAP.sql

```



### 필수 시드 (`REQUIRED_BOOTSTRAP`)



| 구분 | 코드 | 없으면 |

|------|------|--------|

| `LOCATION_TYPE` | UNASSIGNED, CONTAINER, ZONE, ROW, RACK | 위치 트리 불가 |

| `MODEL_TYPE` | PDU, UPS, SENSOR, CDU, RDC, DISTRIBUTION_BOARD, OTHER | 모델 등록 불가 |

| `PROTOCOL_TYPE` | snmp, modbus, mqtt | endpoint·수집 Task 불가 |

| `DEVICE_PAGE` | ENVIRONMENT, COOLING, ANALYSIS, POWER, dashboard | 페이지 위젯 불가 |

| `location_node` | UNASSIGNED | 장비 등록 FK 실패 |



> 기존 DB에 소문자 코드(`pdu`, `zone` …)가 있어도 collation 때문에 중복 INSERT 되지 않는다.



### 샘플 (`samples/`)



| 파일 | 내용 |

|------|------|

| `demo_capabilities_devices.sql` | Zone/Rack + PDU/센서 데모 + endpoint + 확인 SELECT |

| `demo_dashboard_widgets.sql` | dashboard 페이지 위젯·point·device 바인딩 |

| `demo_pdu_snmp_collection.sql` | 단상 PDU(OEM-6375 + Raritan) 모델/장비/수집 Task |



---



## 적용 이력 (수동 기록)



| 버전 | 파일 | 적용일 | 환경 | 비고 |

|------|------|--------|------|------|

| V001 | `V001__create_users_table.sql` | | dcim_new | users 최초 생성 |

| V002 | `V002__create_code_group_table.sql` | | dcim_new | code_group 생성 |

| V003 | `V003__create_common_code_table.sql` | | dcim_new | common_code 생성 (V002 선행) |

| V004 | `V004__create_location_node_table.sql` | | dcim_new | location_node + LOCATION_TYPE·UNASSIGNED 시드 |

| V005 | `V005__create_device_model_tables.sql` | | dcim_new | device_model + MODEL_TYPE·PROTOCOL_TYPE 시드 |

| V006 | `V006__create_device_model_snmp_point.sql` | | dcim_new | device_model_snmp_point |

| V007 | `V007__create_devices_table.sql` | | dcim_new | devices |

| V008 | `V008__add_device_model_device_type_id.sql` | | dcim_new | 기존 DB용 ALTER |

| V018 | `V018__create_page_widget.sql` | | dcim_new | page_widget(+point/device/layout) + device_page 제거 |

| V019 | `V019__page_widget_count_mode.sql` | | dcim_new | page_widget.count_mode / count_model_id |



---



## 관련 문서



- [ERD.md](../docs/ERD.md) — 테이블·컬럼 다이어그램

