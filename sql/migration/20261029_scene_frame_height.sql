-- Preserve the current height of existing frames before allowing it to vary independently.
SET @scene_frame_height_new_column := (
  SELECT COUNT(*) = 0 FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'device_page_scene_item'
    AND COLUMN_NAME = 'size_y'
);

ALTER TABLE `device_page_scene_item`
  ADD COLUMN IF NOT EXISTS `size_y` double NOT NULL DEFAULT 4
  COMMENT '프레임 높이 배율 (size_y / 4)' AFTER `size_x`;

UPDATE `device_page_scene_item`
SET `size_y` = `size_x`
WHERE @scene_frame_height_new_column = 1
  AND `model_key` IN ('SR_INROW_MOLD', 'SR_WALL_COIL');
