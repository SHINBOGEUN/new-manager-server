-- 33_device_group.sql — 물리 위치와 별개로 사용하는 범용 장비 그룹

CREATE TABLE IF NOT EXISTS `device_group` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '장비 그룹 ID',
  `name` varchar(100) NOT NULL COMMENT '그룹명',
  `description` varchar(1000) DEFAULT NULL COMMENT '설명',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '사용 여부 (0=false, 1=true)',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_group_name` (`name`),
  CONSTRAINT `chk_device_group_enabled` CHECK (`enabled` in (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='범용 장비 그룹 (IT, Cooling, A계통 등)';
