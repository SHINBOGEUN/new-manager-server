# MQTT 카탈로그 이관 검증

`mqtt_catalog_migration.sql`은 MariaDB 11.8.2에서 기존 LoRa 구조와 테스트 데이터를 만들고,
실제 이관 SQL을 실행해 ID·값·시각·페이지 표시 설정 보존을 검증한다.
신규 생성 DDL과 이관 결과의 컬럼·인덱스·제약 이름을 비교하며,
중복 식별값·존재하지 않는 장비·인덱스 경로가 없는 구성요소 설정의 저장을 거부하는지도 검사한다.

아래 명령은 외부 포트를 열지 않는 임시 컨테이너에서만 실행한다. 운영 DB에서는 실행하지 않는다.
이미 존재하는 테스트 DB 이름이 있으면 중단하므로 검증을 반복하려면 새 컨테이너를 사용한다.

```powershell
docker run --detach --rm --network none --name dcim-mqtt-migration-test `
  -e MARIADB_ALLOW_EMPTY_ROOT_PASSWORD=1 `
  --mount "type=bind,source=C:\work\dcim\dcim,target=/workspace,readonly" mariadb:11.8.2

# DB 초기화 완료 후 실행한다.
docker exec dcim-mqtt-migration-test mariadb -uroot --abort-source-on-error `
  -e "SOURCE /workspace/sql/tests/mqtt_catalog_migration.sql;"

# 검증 종료 후 임시 컨테이너와 테스트 DB를 정리한다.
docker stop dcim-mqtt-migration-test
```

Manager의 공통 MQTT 도메인·저장소와 LoRa 호환성은 다음 테스트로 검증한다.
저장소 테스트는 명시적인 H2 메모리 DB를 사용한다.

```powershell
mvn clean test "-Dtest=net.vivans.dcim.module.mqtt.**,net.vivans.dcim.module.lora.**,DevicePageMeasurementServiceTest"
```
