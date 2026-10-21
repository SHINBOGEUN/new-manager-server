ALTER TABLE `page_widget`
  DROP CONSTRAINT `chk_page_widget_query_kind`,
  ADD CONSTRAINT `chk_page_widget_query_kind`
    CHECK (`query_kind` in ('last','aggregate','count','chart','calculated','psychrometric','power_distribution','cctv'));

CREATE TABLE IF NOT EXISTS `cctv_camera` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'CCTV 카메라 ID',
  `camera_key` varchar(50) NOT NULL COMMENT '카메라 고유 코드',
  `name` varchar(100) NOT NULL COMMENT '화면 표시 이름',
  `stream_url` varchar(1000) NOT NULL COMMENT '브라우저에서 열 CCTV 보기 URL',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '위젯에서 선택 및 표시할지 여부',
  `sort_order` int(11) NOT NULL DEFAULT 0 COMMENT '카메라 목록 표시 순서',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cctv_camera_camera_key` (`camera_key`),
  CONSTRAINT `chk_cctv_camera_enabled` CHECK (`enabled` in (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='위젯에서 사용할 CCTV 카메라 카탈로그';

INSERT INTO `cctv_camera` (`camera_key`, `name`, `stream_url`, `enabled`, `sort_order`) VALUES
  ('cam1', '1동', 'http://211.239.122.165:8889/cam1/', 1, 1),
  ('cam2', '8동', 'http://211.239.122.165:8889/cam2/', 1, 2)
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `stream_url` = VALUES(`stream_url`),
  `enabled` = VALUES(`enabled`),
  `sort_order` = VALUES(`sort_order`);

CREATE TABLE IF NOT EXISTS `page_widget_cctv` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'CCTV 위젯 카메라 매핑 ID',
  `widget_id` int(11) NOT NULL COMMENT 'page_widget.id',
  `camera_id` int(11) NOT NULL COMMENT 'cctv_camera.id',
  `sort_order` int(11) NOT NULL DEFAULT 0 COMMENT '위젯 내 카메라 전환 버튼 순서',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_page_widget_cctv_widget_camera` (`widget_id`,`camera_id`),
  KEY `idx_page_widget_cctv_camera` (`camera_id`),
  CONSTRAINT `fk_page_widget_cctv_widget` FOREIGN KEY (`widget_id`) REFERENCES `page_widget` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_page_widget_cctv_camera` FOREIGN KEY (`camera_id`) REFERENCES `cctv_camera` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CCTV 위젯과 표시 카메라 연결';
