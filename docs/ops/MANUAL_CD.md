# 세 앱 수동 CD

`Deploy three services`는 Manager 저장소의 Actions 화면에서 `main`을 선택해 수동 실행한다. 이 워크플로는 Manager PR이 `main`에 병합된 후 Actions 메뉴에 나타난다. Manager, Collector, Sensor Data의 현재 `main`을 각각 Java 17로 빌드·테스트한 뒤, 세 JAR와 정확한 커밋 SHA를 한 묶음으로 보관한다. 빌드가 성공해야만 `production` 배포 단계로 넘어간다.

## 배포 범위

- 대상: `/dcim_new/backend/docker-compose.yml`의 `manager-server`, `collector-service`, `sensor-data-service`만.
- 실행 중인 Compose 프로젝트 `dcim-new-backend`만 허용한다. 같은 디렉터리에 남아 있는 다른 `backend` 프로젝트는 건드리지 않는다.
- 기존 서버의 JAR 바인드 마운트와 이미지·환경변수·볼륨·네트워크를 유지한다.
- 프로젝트와 빌드된 JAR의 버전은 `1.0.1`이다. 서버 Compose의 기존 바인드 마운트 파일명은 `new-*-1.0.0.jar`로 유지하되, CD가 그 파일에 **1.0.1 내용**을 설치한다. 파일명은 마운트 경로일 뿐 실행 버전을 나타내지 않는다. 새 현장 설치용 `compose.yaml`은 1.0.1 파일명을 사용한다.
- MQTT 브로커, MariaDB, InfluxDB, 레거시 서비스는 재생성하지 않는다.
- DB 스키마 변경은 이 워크플로에 포함하지 않는다. 필요한 DDL은 별도 승인·적용 후 배포한다.
- `main` 병합은 배포를 시작하지 않는다. `workflow_dispatch`를 사용한다. GitHub의 `production` Environment에 required reviewer를 설정하면 빌드 이후에도 별도 승인을 요구할 수 있다.

## GitHub 설정

Manager 저장소의 `Settings → Environments → production`에 다음 값을 등록한다. 비밀 값은 저장소 파일이나 채팅에 넣지 않는다.

| 종류 | 이름 | 값 |
| --- | --- | --- |
| Variable | `CD_HOST` | 배포 서버의 SSH 호스트명 또는 IP |
| Variable | `CD_PORT` | SSH 포트 번호 |
| Variable | `CD_USER` | 배포 전용 SSH 계정 |
| Variable | `CD_KNOWN_HOSTS` | 검증한 서버 SSH 호스트 공개키의 known_hosts 한 줄. 비표준 포트라면 `[호스트]:포트` 형식 |
| Secret | `CD_SSH_PRIVATE_KEY` | 해당 배포 계정의 전용 SSH 개인키 |

배포 계정의 공개키만 서버 `authorized_keys`에 등록한다. 개인키를 채팅으로 전달하지 않는다. `ssh-keyscan` 결과를 맹신하지 말고 서버의 SSH 호스트 키 지문과 대조한 뒤 `CD_KNOWN_HOSTS`에 등록한다. 배포 계정은 `/dcim_new/backend`의 JAR와 `.cd-*` 디렉터리를 쓸 수 있고, 비대화형으로 `docker compose`를 실행할 수 있어야 한다. Docker 소켓 접근은 강한 권한이므로 전용 계정을 사용하고 접근 범위를 관리한다.

서버에는 `/dcim_new/backend/.env`, `docker-compose.yml`, 기존 앱 JAR 3개가 있어야 한다. `.env`는 서버에만 남기며 GitHub로 복사하지 않는다. SSH가 GitHub-hosted runner에서 서버 포트에 도달할 수 있어야 한다.

## 실행과 복구

1. 세 저장소의 변경을 각각 `main`에 병합한다. Collector·Sensor Data의 CI가 통과했는지 확인한다.
2. Manager 저장소 `Actions → Deploy three services → Run workflow`에서 `main`을 선택한다.
3. `build` 단계가 세 프로젝트를 검증하고 1.0.1 JAR 묶음 및 정확한 커밋 SHA를 담은 `revisions.txt`를 만든다.
4. `production` 승인 규칙을 설정했다면 승인 후 `deploy` 단계가 SSH로 묶음을 업로드한다.
5. 서버 스크립트가 기존 JAR를 `.cd-releases/<run-id>-<attempt>/previous`에 보관하고, 새 JAR를 교체한 다음 앱 서비스 3개만 `--no-deps --force-recreate`한다. JAR 파일을 원자적으로 교체하므로 단순 `restart`가 아니라 컨테이너 재생성이 필요하다.
6. Manager 정적 페이지와 Collector·Sensor Data의 `/api/health`가 응답하는지 확인한다. 실패하면 이전 JAR를 복원하고 앱 3개를 다시 재생성한다. 복구에도 실패하면 Actions 로그를 확인해 수동으로 조치한다.

서버에는 릴리스별 JAR가 보관된다. 오래된 `.cd-releases`는 디스크 사용량을 확인한 후 운영자가 별도로 정리한다. 배포 자동화는 운영 데이터나 이전 릴리스를 자동 삭제하지 않는다.

## 최초 실행 전 점검

- 배포 계정으로 비밀번호 입력 없이 SSH 접속할 수 있는가?
- 배포 계정이 JAR 파일과 `/dcim_new/backend`에 쓸 수 있고 `docker compose ps`를 실행할 수 있는가?
- 서버에 `.env`가 존재하며 `docker compose --env-file .env -f docker-compose.yml config --services`가 성공하는가?
- 앱 Compose 프로젝트가 현재 세 컨테이너를 소유하는가? 스크립트는 불일치 시 JAR를 건드리기 전에 중단한다.
- 현재 실행 중인 앱의 JAR 마운트가 `/dcim_new/backend/new-*-1.0.0.jar`인가? 스크립트는 불일치 시 중단한다.
- GitHub Actions에서 서버 SSH 포트에 도달할 수 있는가?

첫 배포는 운영자가 Actions 로그와 서버의 앱 로그를 함께 보며 진행한다. 현재의 HTTP 점검은 앱 기동 확인이지 실제 센서 수집이나 DB 질의의 종단간 검증은 아니다.
