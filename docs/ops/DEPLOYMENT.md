# Manager Server 배포·운영 가이드

대상은 `new-manager-server`다. Collector(8081)와 Sensor Data(8082)는 별도 서비스이며, Manager는 MariaDB·InfluxDB·Collector·Sensor Data·MQTT Broker에 네트워크로 연결한다.

## 사전 준비

| 대상 | 기본 포트 | 용도 |
|---|---:|---|
| Manager | 8080 | 운영 콘솔, 위젯, 관리 API |
| Collector | 8081 | SNMP 수집 Job 등록·복구 |
| Sensor Data | 8082 | MQTT 센서 데이터 처리·상태 확인 |
| MariaDB | 현장 설정 | 기준 정보·장비·위젯·자산 메타데이터 |
| InfluxDB | 8086 | 장비 측정값·PUE 시계열 |
| MQTT Broker | 1883 | 센서/수집 메시지 |

- Java 17 또는 Docker 런타임을 준비한다.
- Manager에서 Collector·Sensor Data·InfluxDB·MQTT에 접근 가능한지 방화벽과 DNS/IP를 확인한다.
- Compose 배포는 루트의 [.env.example](../../.env.example)를 `.env`로 복사해 서버에서만 관리한다.
- `JWT_SECRET`, `DB_PASS`, `INFLUX_TOKEN`, `COLLECTOR_API_KEY`는 현장마다 별도 값으로 발급한다.

## 빈 현장 신규 설치

### MariaDB 스키마와 기준 카탈로그

`ddl-auto: none`이므로 Manager가 테이블을 만들지 않는다. 빈 `dcim` DB에 아래 순서대로 적용한다.

1. `sql/schema/01_users.sql`부터 `42_device_asset_document.sql`까지 파일명 순서로 실행한다.
2. `sql/seed/model_catalog.sql`을 **빈 DB에서만 한 번** 실행한다.
3. Ops Console에서 관리자 계정, `UNASSIGNED` 위치와 현장별 위치를 등록한다.
4. 현장 장비·Endpoint·수집 작업·PUE 정의·위젯을 등록한다.

PowerShell 예시:

```powershell
Get-ChildItem sql/schema/*_*.sql | Sort-Object Name | ForEach-Object {
  Get-Content $_.FullName -Raw -Encoding UTF8 | mysql -h $env:MARIADB_HOST -P $env:MARIADB_PORT -u $env:DB_USER -p dcim
}
Get-Content sql/seed/model_catalog.sql -Raw -Encoding UTF8 | mysql -h $env:MARIADB_HOST -P $env:MARIADB_PORT -u $env:DB_USER -p dcim
```

`model_catalog.sql`은 공통코드, 모델, 프로토콜, SNMP/Modbus 포인트만 포함한다. 실제 장비 IP, 위치, 수집 주기, 위젯, PUE 정의, Influx 이력은 현장별로 새로 등록한다.

### 파일 영속 경로

자산 이미지와 첨부 문서는 MariaDB가 아니라 파일 시스템에 저장된다.

- 이미지: `ASSET_IMAGE_STORAGE_PATH`
- 문서: `ASSET_DOCUMENT_STORAGE_PATH`
- 운영 권장 경로: OS 백업 대상인 별도 데이터 디스크

DB에는 파일 메타데이터만 저장되므로, DB와 파일 경로는 반드시 같은 시점의 백업본으로 관리한다. 첨부 파일은 최대 20MB이며 파일 형식은 제한하지 않는다.

## 애플리케이션 배포

### Jar 실행

```powershell
$env:SPRING_PROFILES_ACTIVE = "prod"
$env:JAVA_HOME = "C:\\Program Files\\Amazon Corretto\\jdk17"
.\mvnw.cmd clean package -DskipTests
java -jar target\new-manager-server-1.0.0.jar
```

### Docker 실행

`compose.yaml`은 세 Java 프로젝트가 같은 상위 폴더에 있는 구조를 전제로 한다.

```text
Defog ShowRoom/
├─ new-manager-server/       ← compose.yaml, .env
├─ new-collector-server/
└─ new-sensor-data-server/
```

1. `new-manager-server/.env.example`을 `.env`로 복사하고 모든 `CHANGE_ME` 값을 교체한다.
2. 각 Java 프로젝트에서 Jar를 만든다.
3. Manager 프로젝트에서 Compose를 실행한다.

```powershell
Copy-Item .env.example .env
# .env의 CHANGE_ME 값을 실제 현장 Secret으로 교체

Set-Location ..\new-collector-server
.\mvnw.cmd clean package -DskipTests
Set-Location ..\new-sensor-data-server
.\mvnw.cmd clean package -DskipTests
Set-Location ..\new-manager-server
.\mvnw.cmd clean package -DskipTests

docker compose --env-file .env up -d --build
```

Compose는 MariaDB, InfluxDB, MQTT, Collector, Sensor Data, Manager를 함께 기동한다. 현장별 설정값은 모두 `.env`에 두며, Compose에는 서비스 구조·포트·볼륨만 둔다. 서비스 내부 주소는 아래처럼 고정되므로 `.env`에 현장 IP를 넣지 않는다.

| 대상 | 내부 주소 |
|---|---|
| MariaDB | `mariadb:3306` |
| InfluxDB | `http://influxdb:8086` |
| MQTT | `tcp://mqtt:1883` |
| Collector | `http://collector:8081` |
| Sensor Data | `http://sensor-data:8082` |
| Manager | `http://manager:8080` |

호스트에는 Manager `8080`과 외부 센서용 MQTT `1883`만 공개한다. MariaDB와 InfluxDB는 Compose 네트워크 안에서만 사용한다. Dockerfile과 Compose는 로그·자산 파일을 `runtime/` 하위 또는 `.env`에서 지정한 호스트 경로에 보관한다.

MariaDB 초기화 SQL과 모델 카탈로그 seed는 `mariadb-data` 볼륨이 **비어 있을 때만** 자동 실행된다. 이미 데이터가 있는 현장에서 `down -v`로 볼륨을 삭제하면 DB·Influx 데이터가 지워지므로 실행하지 않는다.

## 배포 직후 확인

1. `GET /actuator/health`가 정상 응답하는지 확인한다.
2. `/ops-console.html`에 로그인한다.
3. **수집 상태**에서 Manager, MariaDB, InfluxDB, Collector, Sensor Data, MQTT 상태를 확인한다.
4. 기존 수집 작업이 있으면 **Collector 재동기화**를 한 번 실행하고 Job 연결 수를 확인한다.
5. 장비 한 대의 최신값, PUE 위젯, 자산 이미지/문서 조회를 확인한다.
6. 필요하면 상단 **MQTT 모니터**에서 `#` 또는 원하는 토픽을 구독해 메시지 흐름을 확인한다.

Collector가 Manager보다 늦게 시작해도 Manager는 1분 기본 주기로 Collector 인스턴스 변경을 감지하고 활성 일반 수집 그룹과 PUE 정의를 다시 반영한다.

## 업데이트와 롤백

### 업데이트 전

1. MariaDB 전체 백업을 생성한다.
2. InfluxDB 버전에 맞는 백업 기능으로 버킷을 백업한다.
3. 자산 이미지·문서 경로 전체를 백업한다.
4. 현재 Jar 또는 Docker image 태그와 환경변수 파일을 보관한다.

### 업데이트

- 기존 DB에는 이미 적용된 DDL을 다시 실행하지 않는다.
- 새 스키마 파일이 추가된 경우에만 해당 파일을 DB 백업 후 적용한다.
- 애플리케이션을 교체하고 **배포 직후 확인** 절차를 실행한다.

### 롤백

애플리케이션만 되돌릴 수 있는지 먼저 확인한다. 스키마가 이전 코드와 호환되지 않으면 MariaDB·InfluxDB·첨부 파일을 같은 백업 시점으로 함께 복구한다.

## 운영 점검 기준

| 항목 | 점검 기준 |
|---|---|
| 수집 상태 | 지연·데이터 없음 장비가 없는지, 최신값은 최대 3개 표시인지 |
| Collector 동기화 | 활성 수집 그룹 수와 Collector Job 연결 수가 일치하는지 |
| PUE | 활성 정의가 Collector에 반영되고 InfluxDB에 시계열이 쌓이는지 |
| 파일 저장소 | 이미지·문서 업로드/다운로드와 백업 경로 권한이 정상인지 |
| 로그 | `/app/log` 또는 `LOG_FILE_NAME` 경로의 용량·보존 정책이 적절한지 |

## 책임 범위

- Manager: 기준정보, 장비/자산/위젯/PUE 정의, 조회와 수집 Job 동기화
- Collector: SNMP/Modbus 등 수집 Job 실행과 MQTT 발행
- Sensor Data: MQTT 구독, 센서 데이터 처리·Influx 저장
- InfluxDB: 측정값·PUE 시계열 보관 및 위젯 트렌드 조회
