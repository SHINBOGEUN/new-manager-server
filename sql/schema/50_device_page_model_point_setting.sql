-- 페이지·장비 모델별 측정 포인트의 노출 여부와 표시 순서를 관리합니다.
-- point_id는 프로토콜별 모델 포인트 테이블 중 하나를 가리키므로,
-- 해당 포인트가 지정된 모델과 프로토콜에 속하는지는 애플리케이션 서비스에서 검증합니다.
CREATE TABLE IF NOT EXISTS `device_page_model_point_setting` (
    `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '페이지 포인트 설정 ID',
    `page_code_id` int(11) NOT NULL COMMENT 'common_code.id — DEVICE_PAGE 그룹의 페이지 코드',
    `model_id` int(11) NOT NULL COMMENT 'device_model.id — 같은 모델의 모든 장비 인스턴스가 공유하는 설정',
    `protocol_type_id` int(11) NOT NULL COMMENT 'common_code.id — PROTOCOL_TYPE 그룹의 프로토콜 코드',
    `point_id` int(11) NOT NULL COMMENT '프로토콜별 모델 포인트 ID — 모델·프로토콜 소속 여부는 애플리케이션에서 검증',
    `is_visible` tinyint(1) NOT NULL DEFAULT 0 COMMENT '해당 페이지에 포인트를 표시할지 여부 (0=false, 1=true)',
    `sort_order` int(11) NOT NULL DEFAULT 0 COMMENT '페이지·모델 내 포인트 표시 순서',
    `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
    `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_page_model_protocol_point`
        (`page_code_id`, `model_id`, `protocol_type_id`, `point_id`),
    KEY `idx_page_model_point_setting_model` (`model_id`),
    KEY `idx_page_model_point_setting_protocol` (`protocol_type_id`),
    CONSTRAINT `fk_page_model_point_setting_page`
        FOREIGN KEY (`page_code_id`) REFERENCES `common_code` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_page_model_point_setting_model`
        FOREIGN KEY (`model_id`) REFERENCES `device_model` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_page_model_point_setting_protocol`
        FOREIGN KEY (`protocol_type_id`) REFERENCES `common_code` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `chk_page_model_point_setting_visible` CHECK (`is_visible` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='페이지별 장비 모델 측정 포인트 노출 및 표시 순서 설정';
