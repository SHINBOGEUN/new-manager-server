CREATE TABLE IF NOT EXISTS `pue_definition` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `calculation_cron` varchar(64) NOT NULL DEFAULT '0 */5 * * * *',
  `collection_enabled` tinyint(1) NOT NULL DEFAULT 1,
  `config_version` int(11) NOT NULL DEFAULT 1,
  `collector_job_id` varchar(100) NULL,
  `created_dt` timestamp(6) NULL DEFAULT current_timestamp(6),
  `updated_dt` timestamp(6) NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pue_definition_name` (`name`),
  CONSTRAINT `chk_pue_definition_enabled` CHECK (`collection_enabled` in (0,1)),
  CONSTRAINT `chk_pue_definition_version` CHECK (`config_version` >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
