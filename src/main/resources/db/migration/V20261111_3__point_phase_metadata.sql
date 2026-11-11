-- Phase order is independent of the model-specific label (L1/R/S/A/U, etc.).
INSERT INTO code_group (group_key, group_name)
SELECT 'PHASE', 'Phase Order'
WHERE NOT EXISTS (SELECT 1 FROM code_group WHERE group_key='PHASE');

INSERT INTO code_group (group_key, group_name)
SELECT 'PHASE_NOTATION', 'Phase Notation'
WHERE NOT EXISTS (SELECT 1 FROM code_group WHERE group_key='PHASE_NOTATION');

INSERT INTO common_code (group_id, code, name, sort_order)
SELECT g.id, x.code, x.name, x.sort_order
FROM code_group g
JOIN (
  SELECT 'P1' code, '1st phase' name, 1 sort_order
  UNION ALL SELECT 'P2', '2nd phase', 2
  UNION ALL SELECT 'P3', '3rd phase', 3
  UNION ALL SELECT 'N', 'Neutral', 4
) x ON g.group_key='PHASE'
WHERE NOT EXISTS (SELECT 1 FROM common_code c WHERE c.group_id=g.id AND c.code=x.code);

INSERT INTO common_code (group_id, code, name, sort_order)
SELECT g.id, x.code, x.name, x.sort_order
FROM code_group g
JOIN (
  SELECT 'L123' code, 'L1/L2/L3/N' name, 1 sort_order
  UNION ALL SELECT 'RST', 'R/S/T/N', 2
  UNION ALL SELECT 'ABC', 'A/B/C/N', 3
  UNION ALL SELECT 'UVW', 'U/V/W/N', 4
) x ON g.group_key='PHASE_NOTATION'
WHERE NOT EXISTS (SELECT 1 FROM common_code c WHERE c.group_id=g.id AND c.code=x.code);

ALTER TABLE device_model
  ADD COLUMN IF NOT EXISTS phase_notation_code_id INT NULL COMMENT 'PHASE_NOTATION common_code.id';
ALTER TABLE device_model_snmp_point
  ADD COLUMN IF NOT EXISTS phase_code_id INT NULL COMMENT 'PHASE common_code.id';
ALTER TABLE device_model_modbus_point
  ADD COLUMN IF NOT EXISTS phase_code_id INT NULL COMMENT 'PHASE common_code.id';
ALTER TABLE device_model_mqtt_point
  ADD COLUMN IF NOT EXISTS phase_code_id INT NULL COMMENT 'PHASE common_code.id';

-- Preserve legacy L1_/L2_/L3_/N_ configuration once; R/S/T-style names need manual assignment.
UPDATE device_model_snmp_point p
JOIN code_group g ON g.group_key='PHASE'
JOIN common_code c ON c.group_id=g.id AND c.code=CASE
  WHEN p.name REGEXP '^L1_' THEN 'P1'
  WHEN p.name REGEXP '^L2_' THEN 'P2'
  WHEN p.name REGEXP '^L3_' THEN 'P3'
  WHEN p.name REGEXP '^N_' THEN 'N'
END
SET p.phase_code_id=c.id
WHERE p.phase_code_id IS NULL AND p.name REGEXP '^(L[123]_|N_)';

UPDATE device_model_modbus_point p
JOIN code_group g ON g.group_key='PHASE'
JOIN common_code c ON c.group_id=g.id AND c.code=CASE
  WHEN p.name REGEXP '^L1_' THEN 'P1'
  WHEN p.name REGEXP '^L2_' THEN 'P2'
  WHEN p.name REGEXP '^L3_' THEN 'P3'
  WHEN p.name REGEXP '^N_' THEN 'N'
END
SET p.phase_code_id=c.id
WHERE p.phase_code_id IS NULL AND p.name REGEXP '^(L[123]_|N_)';

UPDATE device_model_mqtt_point p
JOIN code_group g ON g.group_key='PHASE'
JOIN common_code c ON c.group_id=g.id AND c.code=CASE
  WHEN p.point_name REGEXP '^L1_' THEN 'P1'
  WHEN p.point_name REGEXP '^L2_' THEN 'P2'
  WHEN p.point_name REGEXP '^L3_' THEN 'P3'
  WHEN p.point_name REGEXP '^N_' THEN 'N'
END
SET p.phase_code_id=c.id
WHERE p.phase_code_id IS NULL AND p.point_name REGEXP '^(L[123]_|N_)';

-- Models with legacy phase-prefixed points use the L123 notation by default.
UPDATE device_model m
JOIN device_model_protocol mp ON mp.model_id=m.id
LEFT JOIN device_model_snmp_point s ON s.model_protocol_id=mp.id AND s.name REGEXP '^L[123]_'
LEFT JOIN device_model_modbus_point b ON b.model_protocol_id=mp.id AND b.name REGEXP '^L[123]_'
LEFT JOIN device_model_mqtt_point q ON q.device_model_id=m.id AND q.point_name REGEXP '^L[123]_'
JOIN code_group g ON g.group_key='PHASE_NOTATION'
JOIN common_code c ON c.group_id=g.id AND c.code='L123'
SET m.phase_notation_code_id=c.id
WHERE m.phase_notation_code_id IS NULL AND (s.id IS NOT NULL OR b.id IS NOT NULL OR q.id IS NOT NULL);

-- Add foreign keys conditionally so retrying a partially applied migration is safe.
SET @dcim_fk_count = (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA=DATABASE() AND TABLE_NAME='device_model' AND CONSTRAINT_NAME='fk_device_model_phase_notation_code_id');
SET @dcim_ddl = IF(@dcim_fk_count=0,
  'ALTER TABLE device_model ADD CONSTRAINT fk_device_model_phase_notation_code_id FOREIGN KEY (phase_notation_code_id) REFERENCES common_code(id) ON DELETE RESTRICT ON UPDATE CASCADE',
  'SELECT 1');
PREPARE dcim_stmt FROM @dcim_ddl; EXECUTE dcim_stmt; DEALLOCATE PREPARE dcim_stmt;

SET @dcim_fk_count = (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA=DATABASE() AND TABLE_NAME='device_model_snmp_point' AND CONSTRAINT_NAME='fk_snmp_point_phase_code_id');
SET @dcim_ddl = IF(@dcim_fk_count=0,
  'ALTER TABLE device_model_snmp_point ADD CONSTRAINT fk_snmp_point_phase_code_id FOREIGN KEY (phase_code_id) REFERENCES common_code(id) ON DELETE RESTRICT ON UPDATE CASCADE',
  'SELECT 1');
PREPARE dcim_stmt FROM @dcim_ddl; EXECUTE dcim_stmt; DEALLOCATE PREPARE dcim_stmt;

SET @dcim_fk_count = (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA=DATABASE() AND TABLE_NAME='device_model_modbus_point' AND CONSTRAINT_NAME='fk_modbus_point_phase_code_id');
SET @dcim_ddl = IF(@dcim_fk_count=0,
  'ALTER TABLE device_model_modbus_point ADD CONSTRAINT fk_modbus_point_phase_code_id FOREIGN KEY (phase_code_id) REFERENCES common_code(id) ON DELETE RESTRICT ON UPDATE CASCADE',
  'SELECT 1');
PREPARE dcim_stmt FROM @dcim_ddl; EXECUTE dcim_stmt; DEALLOCATE PREPARE dcim_stmt;

SET @dcim_fk_count = (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA=DATABASE() AND TABLE_NAME='device_model_mqtt_point' AND CONSTRAINT_NAME='fk_mqtt_point_phase_code_id');
SET @dcim_ddl = IF(@dcim_fk_count=0,
  'ALTER TABLE device_model_mqtt_point ADD CONSTRAINT fk_mqtt_point_phase_code_id FOREIGN KEY (phase_code_id) REFERENCES common_code(id) ON DELETE RESTRICT ON UPDATE CASCADE',
  'SELECT 1');
PREPARE dcim_stmt FROM @dcim_ddl; EXECUTE dcim_stmt; DEALLOCATE PREPARE dcim_stmt;
