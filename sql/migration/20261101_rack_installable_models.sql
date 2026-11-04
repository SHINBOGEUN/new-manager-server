-- Seed only on the first application. Reruns preserve administrator choices.
SET @rack_installable_added = (SELECT COUNT(*) = 0 FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'scene_model' AND column_name = 'rack_installable');
ALTER TABLE `scene_model` ADD COLUMN IF NOT EXISTS `rack_installable` tinyint(1) NOT NULL DEFAULT 0 COMMENT '랙에 설치 가능한 3D 모델 여부';
UPDATE `scene_model` SET `rack_installable` = 1
WHERE @rack_installable_added = 1 AND `model_key` IN ('SR_GPU_SERVER', 'SR_DLC_SERVER', 'SR_PDU_RACK', 'SR_PDU_VERTICAL');
