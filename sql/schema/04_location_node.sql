-- 04_location_node.sql — dcim_new schema snapshot (FK order)

CREATE TABLE IF NOT EXISTS `location_node` (
  `code` char(10) NOT NULL COMMENT '노드 PK (일반: 10자 Base62 / 시스템: UNASSIGNED)',
  `parent_code` char(10) DEFAULT NULL COMMENT '부모 노드 code (루트는 NULL)',
  `location_type_id` int(11) NOT NULL COMMENT '위치 유형 ID (FK → common_code, LOCATION_TYPE만)',
  `name` varchar(255) NOT NULL COMMENT '노드 표시명',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`code`),
  UNIQUE KEY `uk_location_node_parent_code_name` (`parent_code`,`name`),
  KEY `idx_location_node_parent_code` (`parent_code`),
  KEY `idx_location_node_location_type_id` (`location_type_id`),
  CONSTRAINT `fk_location_node_location_type_id` FOREIGN KEY (`location_type_id`) REFERENCES `common_code` (`id`) ON UPDATE CASCADE,
  CONSTRAINT `fk_location_node_parent_code` FOREIGN KEY (`parent_code`) REFERENCES `location_node` (`code`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='위치 트리 노드'
