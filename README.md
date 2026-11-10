# DCIM Manager Server

DCIM 장비·도면·위젯·계정과 수집 설정을 관리하는 서버입니다.

- 요구 사항: JDK 17, 로컬 실행에 필요한 MariaDB 및 설정값.
- 실행 설정: 작업 폴더의 `config/application.yml`(Git 제외)에 로컬 설정을 입력합니다.
- 실행: Windows `./mvnw.cmd spring-boot:run`, Linux/macOS `./mvnw spring-boot:run`.
- 테스트: Windows `./mvnw.cmd verify`, Linux/macOS `./mvnw verify`.
- Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) (활성화된 경우).
- 기준 문서: [DCIM_NEW 문서](../docs/README.md).
