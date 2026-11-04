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
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6),
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6),
  PRIMARY KEY (`model_key`),
  CONSTRAINT `ck_scene_model_source` CHECK ((`builtin_file` IS NULL) <> (`storage_key` IS NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO `scene_model`
  (`model_key`, `model_name`, `builtin_file`, `file_name`, `width_m`, `height_m`, `depth_m`) VALUES
  ('SR_RACK', '서버 랙 (전면 도어)', 'Inrow_Cooling_Rack.glb', 'Inrow_Cooling_Rack.glb', .9, 2.35, .9),
  ('SR_RDC', 'RDC 장비', 'rdc.glb', 'rdc.glb', 1.2, 2.1, 1.1),
  ('SR_INROW', '인로우 냉각기', 'inrow-cooler.glb', 'inrow-cooler.glb', .9, 2.35, .9),
  ('SR_INROW_MOLD', '인로우 프레임', 'in_row_frame.glb', 'in_row_frame.glb', 1.05, 1.05, 1.45),
  ('SR_INROW_PANEL', '인로우 분전반', 'in_row_distribution_panel.glb', 'in_row_distribution_panel.glb', 1, 1.77, .4),
  ('SR_WALL_COIL', '월 코일 설비', 'wall_coil_mold.glb', 'wall_coil_mold.glb', 2.8, 2.2, 1.6),
  ('SR_DLC_RACK', 'DLC 랙', 'DLC_Rack.glb', 'DLC_Rack.glb', .9, 2.35, .9),
  ('SR_INROW_RACK', '인로우 냉각 랙', 'Inrow_Cooling_Rack.glb', 'Inrow_Cooling_Rack.glb', .9, 2.35, .9),
  ('SR_OCP_RACK', 'OCP 랙', 'OCP_Rack.glb', 'OCP_Rack.glb', .9, 2.35, .9),
  ('SR_RDC_RACK', 'RDC 랙', 'RDC_Rack.glb', 'RDC_Rack.glb', .9, 2.35, .9),
  ('SR_ORV3_RACK', 'ORV3 랙', 'ORV3_Rack.glb', 'ORV3_Rack.glb', .9, 2.35, .9),
  ('SR_IMMERSION', '액침 냉각 설비', 'Immersion_Cooling.glb', 'Immersion_Cooling.glb', 1.4, 1.5, 1.2),
  ('SR_HAC_RACK', 'HAC 랙', 'HAC_Rack.glb', 'HAC_Rack.glb', .9, 2.35, .9),
  ('SR_DLC_CDU', 'DLC CDU', 'DLC_CDU_CHx200.glb', 'DLC_CDU_CHx200.glb', 1.2, 2, 1),
  ('SR_DLC_SERVER', 'DLC 서버', 'DLC_Server_SuperMicro SYS-422GS-NBRT-LCC.glb', 'DLC_Server_SuperMicro SYS-422GS-NBRT-LCC.glb', 1, 1.2, 1);

UPDATE `scene_model` SET `category` = CASE
  WHEN `model_key` = 'SR_INROW_MOLD' THEN '프레임·구조물'
  WHEN `model_key` IN ('SR_RACK', 'SR_INROW_RACK', 'SR_DLC_RACK', 'SR_OCP_RACK', 'SR_RDC_RACK', 'SR_ORV3_RACK', 'SR_HAC_RACK', 'SR_DLC_SERVER') THEN '랙·서버'
  WHEN `model_key` IN ('SR_INROW', 'SR_WALL_COIL', 'SR_IMMERSION', 'SR_DLC_CDU') THEN '냉각 설비'
  WHEN `model_key` IN ('SR_INROW_PANEL', 'SR_RDC') THEN '전력 설비'
  ELSE `category` END
WHERE `category` IN ('기타', '인로우') AND `model_key` LIKE 'SR_%';
