# SQL 스키마 이력

DB 스키마 변경은 **코드와 같이 Git으로 관리**합니다.  
JPA `ddl-auto: none`(운영/개발)이므로, 테이블 생성·변경은 이 폴더의 SQL을 기준으로 적용합니다.

---

## 폴더 구조

```
sql/
├── README.md
├── schema/                  ← 신규 DB용 최종 DDL (01~22, FK 순) — [README](./schema/README.md)
├── history/                 ← 기존 DB 증분 마이그레이션 (V001~V021)
├── seed/
│   └── REQUIRED_BOOTSTRAP.sql   ← 신규 배포 필수 시드
├── dumps/                   ← DB 스냅샷 백업 (mysqldump)
│   └── README.md
└── scripts/
    └── cleanup_business_data.sql  ← 비즈니스 데이터만 삭제 (부트스트랩 유지)
```

---

## 신규 vs 기존 배포

| 대상 | 스키마 | 시드 |
|------|--------|------|
| **빈 DB (신규)** | [`schema/01~22`](./schema/README.md) 번호 순 | [`REQUIRED_BOOTSTRAP.sql`](./seed/REQUIRED_BOOTSTRAP.sql) |
| **history 적용 중인 DB** | 미적용 `history/V00N`만 추가 | 필요 시 bootstrap |

`history/V001~V021`도 빈 DB에 순서대로 적용 가능하나, V008 중복·V013 공백·V012/V015/V020/V021 재실행 불가 등 이슈가 있어 **신규는 `schema/` 권장**. 상세는 [`schema/README.md`](./schema/README.md).

---

## 파일命名 규칙

```
V{번호}__{설명}.sql
```

- **번호는 순차 증가** (이미 적용된 번호는 수정하지 않음)
- **이미 운영에 적용된 파일은 내용을 바꾸지 않고**, 변경이 필요하면 **새 V00N 파일** 추가

---

## 신규 배포 순서

1. **스키마** — [`schema/01~22`](./schema/README.md) 번호 순 (권장) 또는 `history/V001` ~ 최신
2. **필수 시드** — [`seed/REQUIRED_BOOTSTRAP.sql`](./seed/REQUIRED_BOOTSTRAP.sql)
3. **로그인 계정** — `users`는 시드에 없음. Ops Console 또는 API로 생성
4. **현장 데이터** — UI(`/ops-console.html`)로 등록 (샘플 SQL 없음)

```bash
for f in sql/schema/[0-9][0-9]_*.sql; do mysql -h HOST -P PORT -u dcim -p dcim_new < "$f"; done
mysql -h HOST -P PORT -u dcim -p dcim_new < sql/seed/REQUIRED_BOOTSTRAP.sql
```

기존 DB 증분 적용 예:

```bash
mysql -h HOST -P PORT -u dcim -p dcim_new < sql/history/V001__create_users_table.sql
# … 미적용 V00N만 …
```

> **번호 공백:** `V013`은 구 `device_page` 생성 스크립트였으나 V018에서 제거되어 history에서 삭제함.  
> Path는 `V019__device_path_code.sql` 한 파일로 `devices.path_code_id`만 추가 (location에 붙였다 빼는 중간 단계 없음).

### 필수 시드 (`REQUIRED_BOOTSTRAP`)

| 구분 | 코드 | 없으면 |
|------|------|--------|
| `LOCATION_TYPE` | UNASSIGNED, CONTAINER, ZONE, ROW, RACK | 위치 트리 불가 |
| `LOCATION_PATH` | A, B, C | PDU Path 피드 / 차트 `by_path` 그룹 불가 |
| `MODEL_TYPE` | PDU, UPS, SENSOR, … | 모델 등록 불가 |
| `PROTOCOL_TYPE` | snmp, modbus, mqtt | endpoint·수집 Task 불가 |
| `DEVICE_PAGE` | ENVIRONMENT, COOLING, …, dashboard | 페이지 위젯 불가 |
| `location_node` | UNASSIGNED | 장비 등록 FK 실패 |

### 덤프 / 정리

| 경로 | 용도 |
|------|------|
| `dumps/` | mysqldump 백업 |
| `scripts/cleanup_business_data.sql` | 장비·모델·위젯 등만 삭제, 공통코드·UNASSIGNED·users 유지 |

등록 UI: `/ops-console.html`

---

## 적용 이력 (수동 기록)

기존 README의 이력 섹션을 유지하려면 이 파일 하단을 이어서 작성하세요.
