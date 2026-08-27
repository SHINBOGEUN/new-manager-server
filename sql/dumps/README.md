# DB 덤프 (백업)

샘플 SQL 대신, **실제 DB 스냅샷**을 여기에 둡니다.

| 파일 | 설명 |
|------|------|
| `dcim_new_2026-08-27.sql` | 샘플 삭제·정리 직전 `dcim_new` 전체 덤프 |

## 복원

```bash
mysql -h HOST -P PORT -u dcim -p dcim_new < sql/dumps/dcim_new_2026-08-27.sql
```

## 새 덤프

```bash
mysqldump -h HOST -P PORT -u dcim -p --single-transaction --routines --triggers --hex-blob --default-character-set=utf8mb4 dcim_new -r sql/dumps/dcim_new_YYYY-MM-DD.sql
```

신규 등록은 UI: `/ops-console.html`.
