-- Expansion only; no existing devices, memberships or collection switches are changed.
CREATE TABLE IF NOT EXISTS system_setting (
 setting_key VARCHAR(100) NOT NULL PRIMARY KEY, setting_value VARCHAR(500) NOT NULL, description VARCHAR(255) NULL,
 created_dt TIMESTAMP(6) NULL DEFAULT CURRENT_TIMESTAMP(6), updated_dt TIMESTAMP(6) NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS device_collection_setting (
 id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, device_id INT NOT NULL, protocol_type_id INT NOT NULL,
 enabled TINYINT(1) NOT NULL DEFAULT 1 CHECK(enabled IN (0,1)), interval_code_id INT NULL,
 expected_silence_seconds INT NULL CHECK(expected_silence_seconds IS NULL OR expected_silence_seconds>0),
 created_dt TIMESTAMP(6) NULL DEFAULT CURRENT_TIMESTAMP(6), updated_dt TIMESTAMP(6) NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
 CONSTRAINT uk_device_collection_setting_device_protocol UNIQUE(device_id,protocol_type_id),
 KEY idx_device_collection_setting_protocol(protocol_type_id), KEY idx_device_collection_setting_interval(interval_code_id),
 CONSTRAINT fk_device_collection_setting_device FOREIGN KEY(device_id) REFERENCES devices(id) ON DELETE CASCADE ON UPDATE CASCADE,
 CONSTRAINT fk_device_collection_setting_protocol FOREIGN KEY(protocol_type_id) REFERENCES common_code(id) ON UPDATE CASCADE,
 CONSTRAINT fk_device_collection_setting_interval FOREIGN KEY(interval_code_id) REFERENCES common_code(id) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO code_group(group_key,group_name) SELECT 'COLLECTION_INTERVAL','수집 주기' WHERE NOT EXISTS(SELECT 1 FROM code_group WHERE group_key='COLLECTION_INTERVAL');
INSERT INTO common_code(group_id,code,name,sort_order)
 SELECT g.id,v.code,v.name,v.sort_order FROM code_group g JOIN (
 SELECT 'PT1M' code,'1분' name,10 sort_order UNION ALL SELECT 'PT5M','5분',20 UNION ALL SELECT 'PT10M','10분',30
 UNION ALL SELECT 'PT15M','15분',40 UNION ALL SELECT 'PT30M','30분',50 UNION ALL SELECT 'PT1H','60분',60
 ) v ON g.group_key='COLLECTION_INTERVAL'
 WHERE NOT EXISTS(SELECT 1 FROM common_code c WHERE c.group_id=g.id AND c.code=v.code);
INSERT INTO system_setting(setting_key,setting_value,description)
 SELECT 'collection.default.interval','PT1M','신규 장비 기본 수집 주기(COLLECTION_INTERVAL 코드)' WHERE NOT EXISTS(SELECT 1 FROM system_setting WHERE setting_key='collection.default.interval');
INSERT INTO system_setting(setting_key,setting_value,description)
 SELECT 'collection.auto.enabled','false','장비별 수집 설정 자동 반영 스위치' WHERE NOT EXISTS(SELECT 1 FROM system_setting WHERE setting_key='collection.auto.enabled');
