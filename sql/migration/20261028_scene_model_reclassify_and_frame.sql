-- Reclassify previously seeded showroom models by equipment role.
UPDATE `scene_model` SET `category` = CASE
  WHEN `model_key` = 'SR_INROW_MOLD' THEN '프레임·구조물'
  WHEN `model_key` = 'SR_INROW_RACK' THEN '랙·서버'
  WHEN `model_key` = 'SR_INROW' THEN '냉각 설비'
  WHEN `model_key` = 'SR_INROW_PANEL' THEN '전력 설비'
  ELSE `category` END
WHERE `category` = '인로우' AND `model_key` IN ('SR_INROW_MOLD', 'SR_INROW_RACK', 'SR_INROW', 'SR_INROW_PANEL');

-- Existing frame placements used size_x as a uniform scale. Freeze that scale in
-- size_z so future size_x changes extend only the frame length.
UPDATE `device_page_scene_item`
SET `size_z` = `size_x`
WHERE `model_key` = 'SR_INROW_MOLD' AND `size_z` <= 4;
