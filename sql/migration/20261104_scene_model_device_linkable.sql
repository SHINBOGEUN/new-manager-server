-- Add device linking once while preserving subsequent user choices on repeat runs.
SET @device_linkable_first_apply = NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'scene_model' AND column_name = 'device_linkable');
ALTER TABLE `scene_model` ADD COLUMN IF NOT EXISTS `device_linkable` tinyint(1) NOT NULL DEFAULT 0;
UPDATE `scene_model` SET `device_linkable` = 1 WHERE @device_linkable_first_apply = 1 AND `model_key` IN ('SR_RDC','SR_IMMERSION','SR_RDC_RACK');
