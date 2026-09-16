# DCIM Manager Server

Defog ShowRoom용 DCIM(Data Center Infrastructure Management) 백엔드 서버입니다.  
기존 `manager-server`를 DDD 기반 구조로 재작성한 프로젝트입니다.

## 기술 스택

- Java 17, Spring Boot 3.5.3
- MariaDB (JPA), H2 (테스트)
- Spring Security + JWT
- SpringDoc OpenAPI (Swagger)

## 요구 사항

- JDK 17+
- MariaDB (로컬 개발 시 `test` 프로파일)

## 실행

```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

기본 프로파일은 `test`이며, `application-test.yml`의 MariaDB에 연결합니다.

## 테스트

```bash
.\mvnw.cmd verify
```

테스트는 `local` 프로파일 + H2 in-memory DB를 사용합니다. (CI 포함)

| 종류 | 위치 | 설명 |
|------|------|------|
| 단위 | `module/identity/domain`, `application` | 도메인 규칙, 서비스 로직 (Mockito) |
| Repository | `module/identity/infrastructure/persistence` | JPA 저장/조회 |
| 통합 | `module/identity/api` | Auth API MockMvc E2E |

> 로컬에서 JDK 17이 필요합니다. `JAVA_HOME`이 Java 8이면 테스트 실행이 실패합니다.

## API 문서

서버 실행 후:

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/api-docs

## 프로파일

| 프로파일 | 용도 |
|----------|------|
| `test` | 로컬/개발 (기본) |
| `prod` | 운영 (환경변수 필수) |
| `local` | 테스트 전용 H2 (test resources) |

## 문서

- [기능 명세](docs/FEATURES.md) — 기존 manager-server API·기능 정리
- [배포·운영 가이드](docs/ops/DEPLOYMENT.md) — 신규 현장 설치, 환경변수, 백업·복구, 점검 기준

## API prefix

```
/api/manager
```
