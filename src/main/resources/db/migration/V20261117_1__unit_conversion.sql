-- UNIT conversion metadata; no existing point values are changed.
SET @n=(SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='common_code' AND COLUMN_NAME='base_unit_id');
SET @ddl=IF(@n=0,'ALTER TABLE common_code ADD COLUMN base_unit_id INT NULL','SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;
SET @n=(SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='common_code' AND COLUMN_NAME='unit_factor');
SET @ddl=IF(@n=0,'ALTER TABLE common_code ADD COLUMN unit_factor DECIMAL(24,12) NULL','SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;
SET @n=(SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE() AND TABLE_NAME='common_code' AND CONSTRAINT_NAME='fk_common_code_base_unit');
SET @ddl=IF(@n=0,'ALTER TABLE common_code ADD CONSTRAINT fk_common_code_base_unit FOREIGN KEY (base_unit_id) REFERENCES common_code(id) ON UPDATE CASCADE','SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;
SET @unit_group=(SELECT id FROM code_group WHERE group_key='UNIT' LIMIT 1);
INSERT INTO common_code (group_id,code,name,sort_order) SELECT @unit_group,'KW','kW',1000 WHERE @unit_group IS NOT NULL AND NOT EXISTS (SELECT 1 FROM common_code WHERE group_id=@unit_group AND (code='KW' OR BINARY name='kW'));
INSERT INTO common_code (group_id,code,name,sort_order) SELECT @unit_group,'WH','Wh',1001 WHERE @unit_group IS NOT NULL AND NOT EXISTS (SELECT 1 FROM common_code WHERE group_id=@unit_group AND (code='WH' OR BINARY name='Wh'));
SET @kw=(SELECT MIN(id) FROM common_code WHERE group_id=@unit_group AND BINARY name='kW');
SET @kwh=(SELECT MIN(id) FROM common_code WHERE group_id=@unit_group AND BINARY name='kWh');
UPDATE common_code SET base_unit_id=@kw,unit_factor=1 WHERE id=@kw AND base_unit_id IS NULL AND unit_factor IS NULL;
UPDATE common_code SET base_unit_id=@kwh,unit_factor=1 WHERE id=@kwh AND base_unit_id IS NULL AND unit_factor IS NULL;
UPDATE common_code SET base_unit_id=@kw,unit_factor=0.001 WHERE group_id=@unit_group AND BINARY name='W' AND @kw IS NOT NULL AND base_unit_id IS NULL AND unit_factor IS NULL;
UPDATE common_code SET base_unit_id=@kwh,unit_factor=0.001 WHERE group_id=@unit_group AND BINARY name='Wh' AND @kwh IS NOT NULL AND base_unit_id IS NULL AND unit_factor IS NULL;
