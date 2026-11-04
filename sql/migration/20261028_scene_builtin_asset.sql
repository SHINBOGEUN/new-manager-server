-- Apply to existing databases to manage the procedural 3D architecture/factory catalog.
CREATE TABLE IF NOT EXISTS `scene_builtin_asset` (
  `asset_key` varchar(32) NOT NULL,
  `asset_name` varchar(120) NOT NULL,
  `category` varchar(20) NOT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT 1,
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6),
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6),
  PRIMARY KEY (`asset_key`),
  CONSTRAINT `ck_scene_builtin_asset_category` CHECK (`category` IN ('ARCHITECTURE', 'FACTORY'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO `scene_builtin_asset` (`asset_key`, `asset_name`, `category`) VALUES
  ('WALL', '기본 벽', 'ARCHITECTURE'),
  ('WALL_CONCRETE', '콘크리트 벽', 'ARCHITECTURE'),
  ('WALL_GLASS', '유리 벽', 'ARCHITECTURE'),
  ('WALL_GLASS_CLEAR', '통유리 벽', 'ARCHITECTURE'),
  ('WALL_PANEL', '패널 벽', 'ARCHITECTURE'),
  ('DOOR', '문', 'ARCHITECTURE'),
  ('DOOR_DOUBLE', '양문', 'ARCHITECTURE'),
  ('DOOR_SLIDING', '미닫이문', 'ARCHITECTURE'),
  ('WINDOW', '창문', 'ARCHITECTURE'),
  ('WINDOW_DOUBLE', '큰 창문', 'ARCHITECTURE'),
  ('FLOOR', '바닥', 'ARCHITECTURE'),
  ('CEILING', '유리 천장', 'ARCHITECTURE'),
  ('CEILING_CLEAR', '투명 유리 천장', 'ARCHITECTURE'),
  ('LIGHT', '조명', 'ARCHITECTURE'),
  ('PIPE', '배관', 'ARCHITECTURE'),
  ('STAIR', '계단', 'ARCHITECTURE'),
  ('MACHINE', '생산 설비', 'FACTORY'),
  ('CONVEYOR', '컨베이어', 'FACTORY'),
  ('TANK', '저장 탱크', 'FACTORY'),
  ('TABLE', '작업대', 'FACTORY');
