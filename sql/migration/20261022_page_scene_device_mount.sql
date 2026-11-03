-- 장면에 측정 포인트별 마커 대신 장비 하나를 랙/설비에 장착하도록 전환
-- 같은 장비에 기존 포인트별 SENSOR 마커가 여러 개면 가장 먼저 만든 마커만 남긴다.
DELETE duplicate_item
FROM `device_page_scene_item` duplicate_item
JOIN `device_page_scene_item` kept_item
  ON kept_item.`page_code_id` = duplicate_item.`page_code_id`
  AND kept_item.`device_id` = duplicate_item.`device_id`
  AND kept_item.`item_type` = 'SENSOR'
  AND duplicate_item.`item_type` = 'SENSOR'
  AND kept_item.`id` < duplicate_item.`id`;

UPDATE `device_page_scene_item`
SET `item_type` = 'DEVICE',
    `point_name` = NULL,
    `label` = SUBSTRING_INDEX(`label`, ' · ', 1)
WHERE `item_type` = 'SENSOR';

ALTER TABLE `device_page_scene_item`
  DROP INDEX `uk_device_page_scene_item_page_device_point_type`,
  ADD UNIQUE KEY `uk_device_page_scene_item_page_device_type`
    (`page_code_id`, `device_id`, `item_type`);
