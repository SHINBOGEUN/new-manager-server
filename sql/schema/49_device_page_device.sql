-- DEVICE_PAGE(common_code)별 표시 장비. 위젯 소스와 독립적인 페이지 장비 선택.
CREATE TABLE IF NOT EXISTS `device_page_device` (
    `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '페이지 장비 연결 ID',
    `page_code_id` int(11) NOT NULL COMMENT 'common_code.id (DEVICE_PAGE)',
    `device_id` int(11) NOT NULL COMMENT '페이지에 표시할 장비 ID',
    `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '연결 생성 시각',
    `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '연결 수정 시각',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_device_page_device_page_device` (`page_code_id`, `device_id`),
    KEY `idx_device_page_device_device_id` (`device_id`),
    CONSTRAINT `fk_device_page_device_page_code` FOREIGN KEY (`page_code_id`)
        REFERENCES `common_code` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_device_page_device_device` FOREIGN KEY (`device_id`)
        REFERENCES `devices` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='DEVICE_PAGE에 직접 선택한 장비';
