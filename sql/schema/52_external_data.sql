-- 외부 시스템에서 수신한 범용 업무 데이터를 원본 이력으로 저장한다.
-- 업무 기준 날짜/에너지 종류/측정값은 payload_json 내부에 보관한다.
CREATE TABLE IF NOT EXISTS `external_data` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '외부 데이터 수신 레코드 ID',
    `data_category` varchar(50) NOT NULL COMMENT '데이터 분류 (예: BILLING, CONSUMPTION, RATE)',
    `period_type` varchar(30) NOT NULL COMMENT '자료 주기 (예: MONTHLY, DAILY, REALTIME)',
    `payload_json` LONGTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '외부 수신 원본 JSON payload' CHECK (json_valid(`payload_json`)),
    `create_dt` timestamp(6) NOT NULL DEFAULT current_timestamp(6) COMMENT 'Manager가 데이터를 수신해 저장한 시각',
    PRIMARY KEY (`id`),
    KEY `idx_external_data_category_period_created` (`data_category`, `period_type`, `create_dt`, `id`),
  KEY `idx_external_data_created` (`create_dt`, `id`),
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='외부 데이터 JSON 수신 이력';
