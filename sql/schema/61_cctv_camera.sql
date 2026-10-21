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
