-- 임시 테스트 DB 전용. 이관 후 실행.
DELIMITER //
CREATE PROCEDURE assert_source_migration()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM mqtt_source s JOIN mqtt_source_status t ON s.id=t.source_id
                 WHERE s.id=7 AND s.config_version=4 AND s.credential_key='chirpstack'
                   AND s.source_type='CHIRPSTACK_LORA' AND t.status='CONNECTED' AND t.message_count=123) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='LoRa source/state parity failed';
  END IF;
  INSERT INTO mqtt_source (name,source_type,broker_url,topic)
  VALUES ('gpu-test','AGENT','tcp://gpu:1883','gpu/+/telemetry');
  INSERT INTO mqtt_source_status (source_id,status) VALUES (LAST_INSERT_ID(),'DISABLED');
  DELETE FROM mqtt_source WHERE name='gpu-test';
  IF (SELECT COUNT(*) FROM mqtt_source_status) <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Source status FK cascade failed';
  END IF;
END//
DELIMITER ;
CALL assert_source_migration();
DROP PROCEDURE assert_source_migration;
SELECT 'MQTT_SOURCE_MIGRATION_OK' AS result;
