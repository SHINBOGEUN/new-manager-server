-- 장비 인스턴스와 분리된 자산 관리 속성(1 장비 : 1 자산 상세)
-- 기존 운영 DB 이관 시에는 devices의 legacy 자산 컬럼을 이 테이블로 복사한 뒤 애플리케이션을 재기동한다.

CREATE TABLE IF NOT EXISTS `device_asset` (
  `device_id` int(11) NOT NULL COMMENT 'devices.id (PK/FK)',
  `asset_code` varchar(100) DEFAULT NULL COMMENT '내부 자산번호',
  `serial_number` varchar(200) DEFAULT NULL COMMENT '제조사 시리얼번호',
  `asset_status_id` int(11) DEFAULT NULL COMMENT 'ASSET_STATUS common_code.id',
  `asset_color` varchar(20) DEFAULT NULL COMMENT '자산 표시 색상',
  `installed_date` date DEFAULT NULL COMMENT '설치일',
  `asset_manager_name` varchar(100) DEFAULT NULL COMMENT '자산 담당자',
  `supplier_name` varchar(255) DEFAULT NULL COMMENT '공급사',
  `warranty_expires_on` date DEFAULT NULL COMMENT '보증 만료일',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`device_id`),
  UNIQUE KEY `uk_device_asset_asset_code` (`asset_code`),
  KEY `idx_device_asset_status_id` (`asset_status_id`),
  CONSTRAINT `fk_device_asset_device` FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_device_asset_status` FOREIGN KEY (`asset_status_id`) REFERENCES `common_code` (`id`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='장비 자산 관리 속성';
