-- 자산별 첨부 문서 (자산당 여러 파일을 형식 구분 없이 관리)
CREATE TABLE IF NOT EXISTS `device_asset_document` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `device_id` int(11) NOT NULL COMMENT 'devices.id',
  `storage_key` varchar(255) NOT NULL COMMENT '서버 파일 저장 키',
  `original_name` varchar(255) NOT NULL COMMENT '업로드 원본 파일명',
  `content_type` varchar(150) NOT NULL COMMENT 'MIME type',
  `file_size` bigint(20) NOT NULL COMMENT '파일 크기(byte)',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '업로드 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_asset_document_storage_key` (`storage_key`),
  KEY `idx_device_asset_document_device_created` (`device_id`,`created_dt`),
  CONSTRAINT `fk_device_asset_document_device` FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='장비 자산 첨부 문서';
