-- 기존 DB 전용. 신규 DB는 schema/60,61을 사용한다.
-- 적용 전 백업 및 기존 두 테이블 존재/새 테이블 미존재를 확인한다.
-- DDL은 자동 커밋된다. 실패하면 SHOW CREATE TABLE로 적용 단계를 확인하고 재개한다.
-- 행을 복사/삭제하지 않으므로 소스 ID·상태·자격증명 키·버전이 보존된다.
RENAME TABLE `lora_mqtt_source` TO `mqtt_source`,
             `lora_mqtt_source_status` TO `mqtt_source_status`;

ALTER TABLE `mqtt_source`
  RENAME INDEX `uk_lora_mqtt_source_name` TO `uk_mqtt_source_name`,
  DROP CONSTRAINT `chk_lora_mqtt_source_type`,
  DROP CONSTRAINT `chk_lora_mqtt_source_enabled`,
  ADD CONSTRAINT `chk_mqtt_source_type` CHECK (`source_type` IN ('CHIRPSTACK_LORA','AGENT')),
  ADD CONSTRAINT `chk_mqtt_source_enabled` CHECK (`enabled` IN (0,1)),
  COMMENT='공통 MQTT 수집 소스 (LoRa·에이전트)';

ALTER TABLE `mqtt_source_status`
  DROP FOREIGN KEY `fk_lora_mqtt_source_status_source`,
  DROP CONSTRAINT `chk_lora_mqtt_source_status`,
  ADD CONSTRAINT `fk_mqtt_source_status_source` FOREIGN KEY (`source_id`) REFERENCES `mqtt_source` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `chk_mqtt_source_status` CHECK (`status` IN ('NOT_SYNCED','CONNECTING','CONNECTED','DISCONNECTED','ERROR','DISABLED')),
  MODIFY COLUMN `source_id` int(11) NOT NULL COMMENT 'mqtt_source.id (PK/FK)',
  COMMENT='공통 MQTT 수집 소스 연결/수신 상태';
