-- 3D 모델 역할을 모델 키 이름 규칙(SR_*RACK, SR_INROW_PANEL 등) 대신 카탈로그 값으로 관리한다.
-- 최초 적용 및 중단 복구 전용. 성공 후 반복 실행하지 않는다: 의도한 NULL이 기본 역할로 바뀔 수 있다.
-- T-002 Flyway V2에서 적용하며 이 파일이 포함되지 않은 기존 배포 방식으로 운영에 배포하지 않는다.
ALTER TABLE `scene_model` ADD COLUMN IF NOT EXISTS `scene_role` varchar(20) DEFAULT NULL COMMENT '도면 역할: RACK, PANEL, FRAME 또는 NULL';
UPDATE `scene_model` SET `scene_role` = 'RACK' WHERE `scene_role` IS NULL AND `model_key` IN ('SR_RACK','SR_DOUBLE_RACK','SR_DLC_RACK','SR_INROW_RACK','SR_OCP_RACK','SR_RDC_RACK','SR_ORV3_RACK','SR_HAC_RACK');
UPDATE `scene_model` SET `scene_role` = 'PANEL' WHERE `scene_role` IS NULL AND `model_key` = 'SR_INROW_PANEL';
UPDATE `scene_model` SET `scene_role` = 'FRAME' WHERE `scene_role` IS NULL AND `model_key` IN ('SR_INROW_MOLD','SR_WALL_COIL');
