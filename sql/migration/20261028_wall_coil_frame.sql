-- Treat the built-in wall-coil enclosure as a resizable equipment frame.
UPDATE `scene_model`
SET `model_name` = CASE WHEN `model_name` = '월 코일 설비' THEN '월 코일 프레임' ELSE `model_name` END,
    `category` = '프레임·구조물'
WHERE `model_key` = 'SR_WALL_COIL'
  AND `builtin_file` = 'wall_coil_mold.glb'
  AND `storage_key` IS NULL
  AND `category` IN ('냉각 설비', '기타', '인로우', '프레임·구조물');

-- Older placements used size_x for all axes and stored the unused size_z as 4.
-- Keep their visible length when the new independent length control is enabled.
UPDATE `device_page_scene_item`
SET `size_z` = `size_x`
WHERE `model_key` = 'SR_WALL_COIL' AND `size_z` <= 4;
