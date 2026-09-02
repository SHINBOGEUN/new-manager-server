-- 02_code_group.sql — dcim_new schema snapshot (FK order)

CREATE TABLE IF NOT EXISTS `code_group` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '코드 그룹 ID',
  `group_key` varchar(100) NOT NULL COMMENT '그룹 키 (예: DEVICE_TYPE)',
  `group_name` varchar(255) NOT NULL COMMENT '그룹 표시명',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code_group_group_key` (`group_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='공통 코드 그룹'
