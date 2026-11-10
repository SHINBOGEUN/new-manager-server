-- 3D 모델 역할을 모델 키 이름 규칙(SR_*RACK, SR_INROW_PANEL 등) 대신 카탈로그 값으로 관리한다.
-- 처음 적용할 때만 기존 하드코딩과 같은 값으로 채우고, 이후 사용자가 바꾼 값은 재실행해도 유지한다.
SET @scene_role_first_apply = NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'scene_model' AND column_name = 'scene_role');
ALTER TABLE `scene_model` ADD COLUMN IF NOT EXISTS `scene_role` varchar(20) DEFAULT NULL COMMENT '도면 역할: RACK, PANEL, FRAME 또는 NULL';
UPDATE `scene_model` SET `scene_role` = 'RACK' WHERE @scene_role_first_apply = 1 AND `model_key` IN ('SR_RACK','SR_DOUBLE_RACK','SR_DLC_RACK','SR_INROW_RACK','SR_OCP_RACK','SR_RDC_RACK','SR_ORV3_RACK','SR_HAC_RACK');
UPDATE `scene_model` SET `scene_role` = 'PANEL' WHERE @scene_role_first_apply = 1 AND `model_key` = 'SR_INROW_PANEL';
UPDATE `scene_model` SET `scene_role` = 'FRAME' WHERE @scene_role_first_apply = 1 AND `model_key` IN ('SR_INROW_MOLD','SR_WALL_COIL');
