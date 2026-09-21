-- 기존 DB 전용. 신규 설치(수정된 43번 적용) 또는 이미 변경한 DB에서는 실행하지 않습니다.
-- 실행 전 접속 DB를 확인하고 백업합니다. 아래 조회가 0행이어야 합니다.
SELECT r.id, r.endpoint_id
FROM device_modbus_reading r
LEFT JOIN device_endpoint_modbus m ON m.endpoint_id = r.endpoint_id
WHERE m.endpoint_id IS NULL;

-- 고아 reading이 있으면 여기서 중단하고 설정 복구 여부를 결정합니다.
-- SHOW CREATE TABLE device_modbus_reading으로 기존 FK 이름을 확인한 후 실행합니다.
-- MariaDB DDL은 자동 커밋됩니다. 이미 변경된 DB에 재실행하지 않습니다.
ALTER TABLE device_modbus_reading
    DROP FOREIGN KEY fk_device_modbus_reading_endpoint_id,
    ADD CONSTRAINT fk_device_modbus_reading_modbus_endpoint_id
        FOREIGN KEY (endpoint_id)
        REFERENCES device_endpoint_modbus (endpoint_id)
        ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE device_modbus_reading
    MODIFY COLUMN endpoint_id INT NOT NULL
    COMMENT 'device_endpoint_modbus.endpoint_id — 수집 원본 Modbus 설정';
