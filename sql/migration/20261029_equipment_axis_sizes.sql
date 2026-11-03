-- Convert pre-existing uniformly scaled equipment once, preserving frame dimensions.
ALTER TABLE `device_page_scene_item`
  ADD COLUMN IF NOT EXISTS `size_axes_version` tinyint NOT NULL DEFAULT 0;

UPDATE `device_page_scene_item`
SET `size_y` = `size_x`, `size_z` = `size_x`, `size_axes_version` = 1
WHERE `size_axes_version` = 0 AND `item_type` IN ('RACK', 'EQUIPMENT')
  AND `model_key` NOT IN ('SR_INROW_MOLD', 'SR_WALL_COIL', 'FLOOR', 'CEILING', 'CEILING_CLEAR', 'LIGHT', 'PIPE', 'STAIR')
  AND `model_key` NOT LIKE 'WALL%' AND `model_key` NOT LIKE 'DOOR%' AND `model_key` NOT LIKE 'WINDOW%';

UPDATE `device_page_scene_item` SET `size_axes_version` = 1 WHERE `size_axes_version` = 0;
ALTER TABLE `device_page_scene_item` ALTER COLUMN `size_axes_version` SET DEFAULT 1;
