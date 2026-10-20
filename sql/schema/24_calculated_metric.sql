CREATE TABLE IF NOT EXISTS `calculated_metric` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '계산 지표 고유 ID',
  `name` varchar(100) NOT NULL COMMENT '계산 지표 이름',
  `calculation_cron` varchar(64) NOT NULL DEFAULT '0 */5 * * * *' COMMENT '계산 실행 주기 (6필드 cron)',
  `collection_enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '계산 지표 수집 활성 여부',
  `config_version` int(11) NOT NULL DEFAULT 1 COMMENT 'Collector 설정 동기화 버전',
  `collector_job_id` varchar(100) DEFAULT NULL COMMENT 'Collector에 등록된 계산 작업 ID',
  `formula` varchar(500) NOT NULL COMMENT 'alias를 사용하는 계산식',
  `result_unit` varchar(32) DEFAULT NULL COMMENT '계산 결과 단위',
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6) COMMENT '생성 시각',
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_calculated_metric_name` (`name`),
  CONSTRAINT `chk_calculated_metric_enabled` CHECK (`collection_enabled` in (0,1)),
  CONSTRAINT `chk_calculated_metric_version` CHECK (`config_version` >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Collector에서 주기적으로 계산하는 지표 정의';
