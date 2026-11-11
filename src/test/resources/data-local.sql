-- Test-only replacement for DataPointTypeBootstrap. Mirrors V1__baseline.sql.
INSERT INTO code_group (group_key, group_name)
SELECT 'DATA_POINT_TYPE', 'Data Point Type'
WHERE NOT EXISTS (
    SELECT 1 FROM code_group WHERE group_key = 'DATA_POINT_TYPE'
);

INSERT INTO common_code (group_id, code, name, sort_order, threshold_supported)
SELECT g.id, 'POWER', 'POWER', 1, FALSE FROM code_group g
WHERE g.group_key = 'DATA_POINT_TYPE'
  AND NOT EXISTS (SELECT 1 FROM common_code c WHERE c.group_id = g.id AND c.code = 'POWER');
INSERT INTO common_code (group_id, code, name, sort_order, threshold_supported)
SELECT g.id, 'ENERGY', 'ENERGY', 2, FALSE FROM code_group g
WHERE g.group_key = 'DATA_POINT_TYPE'
  AND NOT EXISTS (SELECT 1 FROM common_code c WHERE c.group_id = g.id AND c.code = 'ENERGY');
INSERT INTO common_code (group_id, code, name, sort_order, threshold_supported)
SELECT g.id, 'CURRENT', 'CURRENT', 3, FALSE FROM code_group g
WHERE g.group_key = 'DATA_POINT_TYPE'
  AND NOT EXISTS (SELECT 1 FROM common_code c WHERE c.group_id = g.id AND c.code = 'CURRENT');
INSERT INTO common_code (group_id, code, name, sort_order, threshold_supported)
SELECT g.id, 'VOLTAGE', 'VOLTAGE', 4, FALSE FROM code_group g
WHERE g.group_key = 'DATA_POINT_TYPE'
  AND NOT EXISTS (SELECT 1 FROM common_code c WHERE c.group_id = g.id AND c.code = 'VOLTAGE');
INSERT INTO common_code (group_id, code, name, sort_order, threshold_supported)
SELECT g.id, 'TEMPERATURE', 'TEMPERATURE', 5, FALSE FROM code_group g
WHERE g.group_key = 'DATA_POINT_TYPE'
  AND NOT EXISTS (SELECT 1 FROM common_code c WHERE c.group_id = g.id AND c.code = 'TEMPERATURE');
INSERT INTO common_code (group_id, code, name, sort_order, threshold_supported)
SELECT g.id, 'PRESSURE', 'PRESSURE', 6, FALSE FROM code_group g
WHERE g.group_key = 'DATA_POINT_TYPE'
  AND NOT EXISTS (SELECT 1 FROM common_code c WHERE c.group_id = g.id AND c.code = 'PRESSURE');
INSERT INTO common_code (group_id, code, name, sort_order, threshold_supported)
SELECT g.id, 'FLOW', 'FLOW', 7, FALSE FROM code_group g
WHERE g.group_key = 'DATA_POINT_TYPE'
  AND NOT EXISTS (SELECT 1 FROM common_code c WHERE c.group_id = g.id AND c.code = 'FLOW');
INSERT INTO common_code (group_id, code, name, sort_order, threshold_supported)
SELECT g.id, 'POWER_FACTOR', 'POWER_FACTOR', 8, FALSE FROM code_group g
WHERE g.group_key = 'DATA_POINT_TYPE'
  AND NOT EXISTS (SELECT 1 FROM common_code c WHERE c.group_id = g.id AND c.code = 'POWER_FACTOR');
INSERT INTO common_code (group_id, code, name, sort_order, threshold_supported)
SELECT g.id, 'HUMIDITY', 'HUMIDITY', 9, FALSE FROM code_group g
WHERE g.group_key = 'DATA_POINT_TYPE'
  AND NOT EXISTS (SELECT 1 FROM common_code c WHERE c.group_id = g.id AND c.code = 'HUMIDITY');
INSERT INTO common_code (group_id, code, name, sort_order, threshold_supported)
SELECT g.id, 'UNCLASSIFIED', 'UNCLASSIFIED', 10, FALSE FROM code_group g
WHERE g.group_key = 'DATA_POINT_TYPE'
  AND NOT EXISTS (SELECT 1 FROM common_code c WHERE c.group_id = g.id AND c.code = 'UNCLASSIFIED');
