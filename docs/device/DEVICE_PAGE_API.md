# Device Page (페이지 코드)

UI 페이지는 `DEVICE_PAGE` 그룹의 `common_code`로 둡니다.  
장비 범위는 **`device_page`가 아니라** 페이지 위젯 → `page_widget_device`입니다.

> 필수 공통코드: Ops Console에서 `DEVICE_PAGE` 그룹 등록 ([`sql/README.md`](../../sql/README.md))  
> 매핑/카드: [PAGE_WIDGET_API.md](./PAGE_WIDGET_API.md)  
> DDL: [`15_page_widget.sql`](../../sql/schema/15_page_widget.sql) ~ [`22_page_widget_layout.sql`](../../sql/schema/22_page_widget_layout.sql)

---

## 1. 개요

| 개념 | 설명 |
|------|------|
| **DEVICE_PAGE** | `code_group.group_key` |
| **page code** | `ENVIRONMENT`, `COOLING`, `dashboard`, `POWER` … |
| **장비 소속** | `page_widget` + `page_widget_device` |

```text
common_code (DEVICE_PAGE)
  └─ page_widget
       └─ page_widget_device → devices
```

`GET /devices?pageCode=` / capabilities의 `pageCode` 필터도 **해당 페이지 위젯에 묶인 장비** 기준입니다.

---

## 2. 제거된 API

아래는 V018에서 삭제되었습니다.

| Method | Path |
|--------|------|
| GET/POST/PUT/DELETE | `/api/manager/devices/{deviceId}/pages` |

장비↔페이지를 바꾸려면 위젯 CRUD로 `deviceIds`를 수정하세요.
