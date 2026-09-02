-- 03_common_code.sql — dcim_new schema snapshot (FK order)

CREATE TABLE IF NOT EXISTS `common_code` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '공통 코드 ID',
  `group_id` int(11) NOT NULL COMMENT '코드 그룹 ID (FK)',
  `code` varchar(100) NOT NULL COMMENT '코드 값 (예: ups, pdu)',
  `name` varchar(255) NOT NULL COMMENT '코드 표시명',
  `sort_order` int(11) DEFAULT NULL COMMENT '정렬 순서',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_common_code_group_id_code` (`group_id`,`code`),
  KEY `idx_common_code_group_id` (`group_id`),
  CONSTRAINT `fk_common_code_group_id` FOREIGN KEY (`group_id`) REFERENCES `code_group` (`id`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='공통 코드'
