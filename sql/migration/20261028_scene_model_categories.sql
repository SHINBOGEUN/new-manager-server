-- Existing scene_model catalogs: add editable categories without changing model keys or placements.
ALTER TABLE `scene_model` ADD COLUMN IF NOT EXISTS `category` varchar(50) NOT NULL DEFAULT '기타' AFTER `model_name`;

UPDATE `scene_model` SET `category` = CASE
  WHEN `model_key` = 'SR_INROW_MOLD' THEN '프레임·구조물'
  WHEN `model_key` IN ('SR_RACK', 'SR_INROW_RACK', 'SR_DLC_RACK', 'SR_OCP_RACK', 'SR_RDC_RACK', 'SR_ORV3_RACK', 'SR_HAC_RACK', 'SR_DLC_SERVER') THEN '랙·서버'
  WHEN `model_key` IN ('SR_INROW', 'SR_WALL_COIL', 'SR_IMMERSION', 'SR_DLC_CDU') THEN '냉각 설비'
  WHEN `model_key` IN ('SR_INROW_PANEL', 'SR_RDC') THEN '전력 설비'
  ELSE `category` END
WHERE `category` IN ('기타', '인로우') AND `model_key` LIKE 'SR_%';
