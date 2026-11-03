-- Preserve existing custom-model permissions and specialize known built-ins.
SET @rack_options_first_apply = NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'scene_model' AND column_name = 'rack_u_installable');
ALTER TABLE `scene_model` ADD COLUMN IF NOT EXISTS `rack_u_installable` tinyint(1) NOT NULL DEFAULT 0;
ALTER TABLE `scene_model` ADD COLUMN IF NOT EXISTS `rack_attachment_installable` tinyint(1) NOT NULL DEFAULT 0;
UPDATE `scene_model` SET `rack_u_installable` = `rack_installable`, `rack_attachment_installable` = `rack_installable` WHERE @rack_options_first_apply = 1 AND `rack_installable` = 1 AND `rack_u_installable` = 0 AND `rack_attachment_installable` = 0;
UPDATE `scene_model` SET `rack_u_installable`=0, `rack_attachment_installable`=1 WHERE @rack_options_first_apply = 1 AND `rack_installable`=1 AND `model_key` IN ('SR_PDU_VERTICAL','SR_DRAGINO_TH','SR_DRAGINO_DOOR');
UPDATE `scene_model` SET `rack_u_installable`=1, `rack_attachment_installable`=0 WHERE @rack_options_first_apply = 1 AND `rack_installable`=1 AND `model_key` IN ('SR_GPU_SERVER','SR_DLC_SERVER','SR_PDU_RACK');

