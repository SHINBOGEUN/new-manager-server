-- 페이지별 보기 모드와 3D 공간 배치 편집 설정
CREATE TABLE IF NOT EXISTS `device_page_display_config` (
  `page_code_id` int(11) NOT NULL COMMENT 'DEVICE_PAGE 공통코드 ID',
  `view_mode` varchar(16) NOT NULL COMMENT '페이지 표시 방식 (DATA/PLAN_2D/SCENE_3D)',
  `layout_asset_key` varchar(120) DEFAULT NULL COMMENT '선택한 평면도/장면 자산 경로',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`page_code_id`),
  CONSTRAINT `fk_device_page_display_config_page` FOREIGN KEY (`page_code_id`)
    REFERENCES `common_code` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='페이지별 데이터/2D/3D 표시 방식';

CREATE TABLE IF NOT EXISTS `device_page_scene_item` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '장면 객체 ID',
  `page_code_id` int(11) NOT NULL COMMENT '객체가 배치되는 DEVICE_PAGE 공통코드 ID',
  `parent_item_id` int(11) DEFAULT NULL COMMENT '부모 객체 ID (센서를 장착한 랙)',
  `device_id` int(11) DEFAULT NULL COMMENT '연결된 장비 ID (장면용 빈 랙은 NULL)',
  `item_type` varchar(16) NOT NULL COMMENT '장면 객체 종류 (RACK/EQUIPMENT/DEVICE/SENSOR)',
  `point_name` varchar(100) DEFAULT NULL COMMENT '센서 마커가 표시할 측정 포인트 이름',
  `label` varchar(120) NOT NULL COMMENT '장면에 표시할 객체 이름',
  `model_key` varchar(32) NOT NULL COMMENT '장면 모델 카탈로그 키 (랙/장비/벽/문/창문/천장/바닥/조명/배관 등)',
  `position_x` double NOT NULL COMMENT '장면 X 좌표(랙/장비: 방 기준 미터, 센서: 랙 기준 미터)',
  `position_y` double NOT NULL COMMENT '장면 Y 높이(랙/장비: 방 기준 미터, 센서: 랙 기준 미터)',
  `position_z` double NOT NULL COMMENT '장면 Z 좌표(랙/장비: 방 기준 미터, 센서: 랙 기준 미터)',
  `rotation_y` double NOT NULL DEFAULT 0 COMMENT 'Y축 회전 각도(도)',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`id`),
  KEY `idx_device_page_scene_item_page` (`page_code_id`),
  KEY `idx_device_page_scene_item_parent` (`parent_item_id`),
  KEY `idx_device_page_scene_item_device` (`device_id`),
  UNIQUE KEY `uk_device_page_scene_item_page_device_type` (`page_code_id`, `device_id`, `item_type`),
  CONSTRAINT `fk_device_page_scene_item_page` FOREIGN KEY (`page_code_id`)
    REFERENCES `common_code` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_device_page_scene_item_parent` FOREIGN KEY (`parent_item_id`)
    REFERENCES `device_page_scene_item` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_device_page_scene_item_device` FOREIGN KEY (`device_id`)
    REFERENCES `devices` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='페이지 2D/3D 장면의 공간 부품·장비와 장비 부착 위치';

-- Replaced by page-owned scene objects; the old per-device card position model is no longer used.
DROP TABLE IF EXISTS `device_page_device_display`;
