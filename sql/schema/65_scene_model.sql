-- 3D 도면 모델 카탈로그. 기존 SR_* 키를 유지해 저장된 도면과 호환한다.
CREATE TABLE IF NOT EXISTS `scene_model` (
  `model_key` varchar(32) NOT NULL,
  `model_name` varchar(120) NOT NULL,
  `category` varchar(50) NOT NULL DEFAULT '기타',
  `builtin_file` varchar(255) DEFAULT NULL,
  `storage_key` varchar(255) DEFAULT NULL,
  `file_name` varchar(255) NOT NULL,
  `width_m` double NOT NULL,
  `height_m` double NOT NULL,
  `depth_m` double NOT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT 1,
  `rack_u_installable` tinyint(1) NOT NULL DEFAULT 0,
  `rack_attachment_installable` tinyint(1) NOT NULL DEFAULT 0,
  `device_linkable` tinyint(1) NOT NULL DEFAULT 0,
  `panel_installable` tinyint(1) NOT NULL DEFAULT 0,
  `rack_installable` tinyint(1) NOT NULL DEFAULT 0 COMMENT '랙에 설치 가능한 3D 모델 여부',
  `scene_role` varchar(20) DEFAULT NULL COMMENT '도면 역할: RACK, PANEL, FRAME 또는 NULL',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6),
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6),
  PRIMARY KEY (`model_key`),
  CONSTRAINT `ck_scene_model_source` CHECK ((`builtin_file` IS NULL) <> (`storage_key` IS NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @scene_model_initial_seed = (SELECT COUNT(*) = 0 FROM `scene_model`);
INSERT IGNORE INTO `scene_model`
  (`model_key`, `model_name`, `builtin_file`, `file_name`, `width_m`, `height_m`, `depth_m`) VALUES
  ('SR_RACK', '서버 랙 (전면 도어)', 'Inrow_Cooling_Rack.glb', 'Inrow_Cooling_Rack.glb', .9, 2.35, .9),
  ('SR_RDC', 'RDC 장비', 'rdc.glb', 'rdc.glb', 1.2, 2.1, 1.1),
  ('SR_INROW', '인로우 냉각기', 'inrow-cooler.glb', 'inrow-cooler.glb', .9, 2.35, .9),
  ('SR_INROW_MOLD', '인로우 프레임', 'in_row_frame.glb', 'in_row_frame.glb', 1.05, 1.05, 1.45),
  ('SR_INROW_PANEL', '인로우 분전반', 'in_row_distribution_panel.glb', 'in_row_distribution_panel.glb', 1, 1.77, .4),
  ('SR_WALL_COIL', '월 코일 프레임', 'wall_coil_mold.glb', 'wall_coil_mold.glb', 2.8, 2.2, 1.6),
  ('SR_DLC_RACK', 'DLC 랙', 'DLC_Rack.glb', 'DLC_Rack.glb', .9, 2.35, .9),
  ('SR_INROW_RACK', '인로우 냉각 랙', 'Inrow_Cooling_Rack.glb', 'Inrow_Cooling_Rack.glb', .9, 2.35, .9),
  ('SR_OCP_RACK', 'OCP 랙', 'OCP_Rack.glb', 'OCP_Rack.glb', .9, 2.35, .9),
  ('SR_RDC_RACK', 'RDC 랙', 'RDC_Rack.glb', 'RDC_Rack.glb', .9, 2.35, .9),
  ('SR_ORV3_RACK', 'ORV3 랙', 'ORV3_Rack.glb', 'ORV3_Rack.glb', .9, 2.35, .9),
  ('SR_IMMERSION', '액침 냉각 설비', 'Immersion_Cooling.glb', 'Immersion_Cooling.glb', 1.4, 1.5, 1.2),
  ('SR_HAC_RACK', 'HAC 랙', 'HAC_Rack.glb', 'HAC_Rack.glb', .9, 2.35, .9),
  ('SR_DLC_CDU', 'DLC CDU', 'DLC_CDU_CHx200.glb', 'DLC_CDU_CHx200.glb', 1.2, 2, 1),
  ('SR_DLC_SERVER', 'DLC 서버', 'DLC_Server_SuperMicro SYS-422GS-NBRT-LCC.glb', 'DLC_Server_SuperMicro SYS-422GS-NBRT-LCC.glb', 1, 1.2, 1),
  ('SR_PDU_RACK', '랙 장착형 PDU', 'pdu_rack_horizontal.glb', 'pdu_rack_horizontal.glb', .482, .11, .24),
  ('SR_PDU_VERTICAL', '세로형 PDU', 'pdu_rack_vertical.glb', 'pdu_rack_vertical.glb', .1, 1.6, .1),
  ('SR_GPU_SERVER', 'GPU 서버 (4U 예시)', 'gpu_server_rack.glb', 'gpu_server_rack.glb', .482, .178, .72),
  ('SR_DRAGINO_TH', 'Dragino 온습도 센서', 'dragino_temperature_humidity.glb', 'dragino_temperature_humidity.glb', .068, .12, .027),
  ('SR_DRAGINO_DOOR', 'Dragino 도어 센서', 'dragino_door_contact.glb', 'dragino_door_contact.glb', .13, .10, .03),
  ('SR_DOUBLE_RACK', '서버 랙 (양문형)', 'rack_double_door.glb', 'rack_double_door.glb', .9, 2.35, .9),
  ('SR_ACCURA_2350_1P', 'ACCURA 2350 단상', 'accura_2350_1p.glb', 'accura_2350_1p.glb', .045, .105, .045),
  ('SR_ACCURA_2350_3P', 'ACCURA 2350 3상', 'accura_2350_3p.glb', 'accura_2350_3p.glb', .060, .105, .045),
  ('SR_PANEL_BREAKER', '분전반 차단기', 'panel_breaker_3p.glb', 'panel_breaker_3p.glb', .095, .11, .075),
  ('SR_PANEL_BRANCH', '분전반 분기 차단기', 'panel_breaker_3p.glb', 'panel_breaker_3p.glb', .34, .14, .12),
  ('SR_PANEL_MAIN', '분전반 메인 차단기', 'panel_main_breaker.glb', 'panel_main_breaker.glb', .37, .31, .13),
  ('SR_PANEL_METER', '분전반 계기', 'panel_meter.glb', 'panel_meter.glb', .36, .23, .12),
  ('SR_PANEL_BUSBAR', '분전반 부스바', 'panel_busbar.glb', 'panel_busbar.glb', .06, .29, .04),
  ('SR_PANEL_WIRE', '분전반 배선', 'panel_wire.glb', 'panel_wire.glb', .08, .13, .012);

UPDATE `scene_model` SET `category` = CASE
  WHEN `model_key` IN ('SR_DRAGINO_TH', 'SR_DRAGINO_DOOR', 'SR_ACCURA_2350_1P', 'SR_ACCURA_2350_3P') THEN '센서'
  WHEN `model_key` IN ('SR_INROW_MOLD', 'SR_WALL_COIL') THEN '프레임·구조물'
  WHEN `model_key` IN ('SR_RACK', 'SR_DOUBLE_RACK', 'SR_INROW_RACK', 'SR_DLC_RACK', 'SR_OCP_RACK', 'SR_RDC_RACK', 'SR_ORV3_RACK', 'SR_HAC_RACK', 'SR_DLC_SERVER', 'SR_GPU_SERVER') THEN '랙·서버'
  WHEN `model_key` IN ('SR_INROW', 'SR_IMMERSION', 'SR_DLC_CDU') THEN '냉각 설비'
  WHEN `model_key` IN ('SR_INROW_PANEL', 'SR_RDC', 'SR_PDU_RACK', 'SR_PDU_VERTICAL') THEN '전력 설비'
  ELSE `category` END
WHERE `category` IN ('기타', '인로우') AND `model_key` LIKE 'SR_%';

UPDATE `scene_model` SET `rack_installable` = 1
WHERE @scene_model_initial_seed = 1 AND `model_key` IN ('SR_GPU_SERVER', 'SR_DLC_SERVER', 'SR_PDU_RACK', 'SR_PDU_VERTICAL', 'SR_DRAGINO_TH', 'SR_DRAGINO_DOOR');

UPDATE `scene_model` SET `rack_u_installable` = 1 WHERE @scene_model_initial_seed = 1 AND `model_key` IN ('SR_GPU_SERVER','SR_DLC_SERVER','SR_PDU_RACK');
UPDATE `scene_model` SET `rack_attachment_installable` = 1 WHERE @scene_model_initial_seed = 1 AND `model_key` IN ('SR_PDU_VERTICAL','SR_DRAGINO_TH','SR_DRAGINO_DOOR');
UPDATE `scene_model` SET `device_linkable` = 1 WHERE @scene_model_initial_seed = 1 AND `model_key` IN ('SR_RDC','SR_IMMERSION','SR_RDC_RACK');
UPDATE `scene_model` SET `panel_installable` = 1 WHERE @scene_model_initial_seed = 1 AND `model_key` IN ('SR_ACCURA_2350_1P','SR_ACCURA_2350_3P','SR_PANEL_BREAKER','SR_PANEL_BRANCH','SR_PANEL_MAIN','SR_PANEL_METER','SR_PANEL_BUSBAR','SR_PANEL_WIRE');
UPDATE `scene_model` SET `scene_role` = 'RACK' WHERE @scene_model_initial_seed = 1 AND `model_key` IN ('SR_RACK','SR_DOUBLE_RACK','SR_DLC_RACK','SR_INROW_RACK','SR_OCP_RACK','SR_RDC_RACK','SR_ORV3_RACK','SR_HAC_RACK');
UPDATE `scene_model` SET `scene_role` = 'PANEL' WHERE @scene_model_initial_seed = 1 AND `model_key` = 'SR_INROW_PANEL';
UPDATE `scene_model` SET `scene_role` = 'FRAME' WHERE @scene_model_initial_seed = 1 AND `model_key` IN ('SR_INROW_MOLD','SR_WALL_COIL');
