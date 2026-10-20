-- 수동 회귀 검증용. 격리된 테스트 MariaDB에서만 실행한다 (운영 DB용 아님).
-- 실행: mariadb -uroot -e "SOURCE /workspace/sql/tests/mqtt_catalog_migration.sql"
CREATE DATABASE dcim_mqtt_legacy_test CHARACTER SET utf8mb4;
USE dcim_mqtt_legacy_test;
CREATE TABLE devices (id int PRIMARY KEY) ENGINE=InnoDB;
CREATE TABLE device_model (id int PRIMARY KEY) ENGINE=InnoDB;
CREATE TABLE common_code (id int PRIMARY KEY) ENGINE=InnoDB;
SOURCE /workspace/sql/schema/44_device_lora_endpoint.sql;
SOURCE /workspace/sql/schema/45_device_model_lora_point.sql;
SOURCE /workspace/sql/schema/50_device_page_model_point_setting.sql;
INSERT INTO devices VALUES (40),(41);
INSERT INTO device_model VALUES (8),(9);
INSERT INTO common_code VALUES (1),(2),(3),(4),(5);
INSERT INTO device_lora_endpoint(id,device_id,id_type,external_id,normalized_external_id,enabled,created_dt,updated_dt)
VALUES (12,40,'DEV_EUI','24:E1:24:71:0C:12:34:56','24E124710C123456',1,'2026-09-01 00:00:00','2026-09-02 00:00:00'),
       (20,41,'DEVICE_NAME','Dragino-02','DRAGINO02',0,'2026-09-01 00:00:00','2026-09-02 00:00:00');
INSERT INTO device_model_lora_point(id,device_model_id,payload_field,point_name,data_point_type_id,category_code_id,unit_code_id,scale,value_map,enabled,created_dt,updated_dt)
VALUES (7,8,'object.TempC_SHT','TEMPERATURE',3,5,4,0.1,NULL,1,'2026-09-01 00:00:00','2026-09-02 00:00:00'),
       (15,9,'object.Exti_pin_level','DOOR',3,5,NULL,1.0,'{"High":1,"Low":0}',0,'2026-09-01 00:00:00','2026-09-02 00:00:00');
INSERT INTO device_page_model_point_setting(page_code_id,model_id,protocol_type_id,point_id,is_visible,sort_order)
VALUES (1,8,2,7,1,2),(1,9,2,15,0,3);
CREATE TABLE identity_before AS SELECT * FROM device_lora_endpoint;
CREATE TABLE point_before AS SELECT * FROM device_model_lora_point;
CREATE TABLE page_before AS SELECT * FROM device_page_model_point_setting;
SOURCE /workspace/sql/migration/20261014_unify_lora_mqtt.sql;

DELIMITER //
CREATE PROCEDURE verify_mqtt_migration()
BEGIN
  IF (SELECT COUNT(*) FROM device_mqtt_identity) <> 2 OR (SELECT COUNT(*) FROM device_model_mqtt_point) <> 2 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='LoRa row count changed';
  END IF;
  IF EXISTS (SELECT 1 FROM identity_before b LEFT JOIN device_mqtt_identity a ON a.id=b.id
     WHERE a.id IS NULL OR a.device_id<>b.device_id OR a.identity_type<>b.id_type
       OR a.external_id<>b.external_id OR a.normalized_external_id<>b.normalized_external_id
       OR a.enabled<>b.enabled OR NOT(a.created_dt<=>b.created_dt) OR NOT(a.updated_dt<=>b.updated_dt)) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Identity values changed';
  END IF;
  IF EXISTS (SELECT 1 FROM point_before b LEFT JOIN device_model_mqtt_point a ON a.id=b.id
     WHERE a.id IS NULL OR a.device_model_id<>b.device_model_id OR a.payload_field<>b.payload_field
       OR a.point_name<>b.point_name OR a.data_point_type_id<>b.data_point_type_id
       OR NOT(a.unit_code_id<=>b.unit_code_id) OR NOT(a.category_code_id<=>b.category_code_id)
       OR NOT(a.scale<=>b.scale) OR NOT(a.value_map<=>b.value_map) OR a.enabled<>b.enabled
       OR NOT(a.created_dt<=>b.created_dt) OR NOT(a.updated_dt<=>b.updated_dt)
       OR a.component_scope<>'DEVICE' OR a.component_path<>'' OR a.component_type IS NOT NULL
       OR a.component_index_field IS NOT NULL OR a.component_uuid_field IS NOT NULL) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Point IDs or values changed';
  END IF;
  IF EXISTS (SELECT 1 FROM page_before b LEFT JOIN device_page_model_point_setting a ON a.id=b.id
     WHERE a.id IS NULL OR a.point_id<>b.point_id OR a.is_visible<>b.is_visible OR a.sort_order<>b.sort_order) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Page configuration changed';
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE()
       AND table_name IN ('device_lora_endpoint','device_model_lora_point')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Legacy table still exists';
  END IF;
END//
DELIMITER ;
CALL verify_mqtt_migration();
INSERT INTO device_mqtt_identity(device_id,identity_type,external_id,normalized_external_id)
VALUES (40,'MAC','9c:6b:00:9b:4b:d7','9C6B009B4BD7'),(40,'SERIAL','Ab-01','Ab-01'),(41,'SERIAL','ab-01','ab-01');
UPDATE device_mqtt_identity SET external_id='AA:BB:CC:DD:EE:FF',normalized_external_id='AABBCCDDEEFF'
WHERE device_id=40 AND identity_type='MAC';
INSERT INTO device_model_mqtt_point(device_model_id,payload_field,point_name,data_point_type_id,component_scope,component_type,component_path,component_index_field,component_uuid_field)
VALUES (8,'gpu_temperature','gpu_temperature',3,'COMPONENT','GPU','gpus','gpu_index','gpu_uuid');

DELIMITER //
CREATE PROCEDURE verify_mqtt_constraints()
BEGIN
  DECLARE rejected boolean DEFAULT false;
  BEGIN
    DECLARE CONTINUE HANDLER FOR SQLSTATE '23000' SET rejected=true;
    INSERT INTO device_mqtt_identity(device_id,identity_type,external_id,normalized_external_id,enabled)
    VALUES (41,'MAC','AA:BB:CC:DD:EE:FF','AABBCCDDEEFF',0);
  END;
  IF NOT rejected THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Duplicate identity accepted'; END IF;
  SET rejected=false;
  BEGIN
    DECLARE CONTINUE HANDLER FOR SQLSTATE '23000' SET rejected=true;
    INSERT INTO device_mqtt_identity(device_id,identity_type,external_id,normalized_external_id)
    VALUES (999,'MAC','001122334455','001122334455');
  END;
  IF NOT rejected THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Unknown device accepted'; END IF;
  SET rejected=false;
  BEGIN
    DECLARE CONTINUE HANDLER FOR SQLSTATE '23000' SET rejected=true;
    INSERT INTO device_model_mqtt_point(device_model_id,payload_field,point_name,data_point_type_id,component_scope,component_type,component_path)
    VALUES (8,'fan_speed','fan_speed',3,'COMPONENT','GPU','gpus');
  END;
  IF NOT rejected THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Component without index field accepted'; END IF;
END//
DELIMITER ;
CALL verify_mqtt_constraints();

CREATE DATABASE dcim_mqtt_fresh_test CHARACTER SET utf8mb4;
USE dcim_mqtt_fresh_test;
CREATE TABLE devices (id int PRIMARY KEY) ENGINE=InnoDB;
CREATE TABLE device_model (id int PRIMARY KEY) ENGINE=InnoDB;
CREATE TABLE common_code (id int PRIMARY KEY) ENGINE=InnoDB;
SOURCE /workspace/sql/schema/53_device_mqtt_identity.sql;
SOURCE /workspace/sql/schema/54_device_model_mqtt_point.sql;

-- 정보 스키마로 신규 DDL과 이관 결과의 컬럼/인덱스/제약 이름을 비교한다.
DELIMITER //
CREATE PROCEDURE verify_mqtt_schema_parity()
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns a LEFT JOIN information_schema.columns b
      ON b.table_schema='dcim_mqtt_legacy_test' AND a.table_name=b.table_name AND a.column_name=b.column_name
    WHERE a.table_schema=DATABASE() AND a.table_name IN ('device_mqtt_identity','device_model_mqtt_point')
      AND (b.column_name IS NULL OR a.ordinal_position<>b.ordinal_position OR a.column_type<>b.column_type
        OR a.is_nullable<>b.is_nullable OR NOT(a.column_default<=>b.column_default)
        OR NOT(a.collation_name<=>b.collation_name))
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Fresh and migrated column schema differ';
  END IF;
  IF EXISTS (
    SELECT 1 FROM information_schema.statistics a LEFT JOIN information_schema.statistics b
      ON b.table_schema='dcim_mqtt_legacy_test' AND a.table_name=b.table_name
        AND a.index_name=b.index_name AND a.seq_in_index=b.seq_in_index
    WHERE a.table_schema=DATABASE() AND a.table_name IN ('device_mqtt_identity','device_model_mqtt_point')
      AND (b.index_name IS NULL OR a.column_name<>b.column_name OR a.non_unique<>b.non_unique)
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Fresh and migrated indexes differ';
  END IF;
  IF EXISTS (
    SELECT 1 FROM information_schema.table_constraints a LEFT JOIN information_schema.table_constraints b
      ON b.table_schema='dcim_mqtt_legacy_test' AND a.table_name=b.table_name AND a.constraint_name=b.constraint_name
    WHERE a.table_schema=DATABASE() AND a.table_name IN ('device_mqtt_identity','device_model_mqtt_point')
      AND (b.constraint_name IS NULL OR a.constraint_type<>b.constraint_type)
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Fresh and migrated constraint schema differ';
  END IF;
END//
DELIMITER ;
CALL verify_mqtt_schema_parity();
SELECT 'MQTT migration and fresh schema verified' AS result;
