ALTER TABLE `scene_model` ADD COLUMN IF NOT EXISTS `panel_installable` tinyint(1) NOT NULL DEFAULT 0;
UPDATE `scene_model` SET `panel_installable` = 1 WHERE `model_key` IN ('SR_ACCURA_2350_1P','SR_ACCURA_2350_3P');
