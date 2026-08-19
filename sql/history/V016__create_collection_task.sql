-- =============================================================================
-- V016: collection_task / group / device 테이블 생성
-- =============================================================================
-- 작성일  : 2026-08-19
-- 대상 DB : MariaDB (dcim_new)
--
-- 적용 방법 (예시):
--   mysql -h HOST -P PORT -u dcim -p dcim_new < sql/history/V016__create_collection_task.sql
--
-- 선행 조건: V003 (common_code), V005 (device_model), V007 (devices)
--
-- 설계 문서: docs/collection-task/COLLECTION_TASK_DESIGN.md v4
--
-- 역할:
--   - collection_task: 모델+프로토콜당 수집 Task 1개
--   - collection_task_group: Task 안 주기 그룹 (1분/5분 등)
--   - collection_task_device: 그룹에 속한 장비
-- =============================================================================

CREATE TABLE IF NOT EXISTS collection_task (
    id                INT           NOT NULL AUTO_INCREMENT  COMMENT 'Task ID',
    name              VARCHAR(100)  NOT NULL                 COMMENT 'Task 이름',
    model_id          INT           NOT NULL                 COMMENT 'device_model.id',
    script_type_id    INT           NOT NULL                 COMMENT 'common_code.id (PROTOCOL_TYPE)',
    active            TINYINT(1)    NOT NULL DEFAULT 1       COMMENT '활성 여부',
    created_dt        TIMESTAMP(6)  NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_dt        TIMESTAMP(6)  NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_collection_task_model_script (model_id, script_type_id),
    KEY idx_collection_task_script_type_id (script_type_id),
    KEY idx_collection_task_active (active),
    CONSTRAINT fk_collection_task_model_id
        FOREIGN KEY (model_id) REFERENCES device_model (id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT fk_collection_task_script_type_id
        FOREIGN KEY (script_type_id) REFERENCES common_code (id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT chk_collection_task_active CHECK (active IN (0, 1))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='모델+프로토콜당 수집 Task 1개';

CREATE TABLE IF NOT EXISTS collection_task_group (
    id                 INT           NOT NULL AUTO_INCREMENT  COMMENT '그룹 ID',
    task_id            INT           NOT NULL                 COMMENT 'collection_task.id',
    name               VARCHAR(100)  NOT NULL                 COMMENT '그룹 이름',
    cron_expression    VARCHAR(100)  NOT NULL                 COMMENT '수집 주기 cron',
    generated_spec     LONGTEXT      NULL                     COMMENT '그룹 수집 JSON spec',
    collector_job_id   VARCHAR(100)  NULL                     COMMENT 'collector job ID',
    active             TINYINT(1)    NOT NULL DEFAULT 1       COMMENT '그룹 활성 여부',
    created_dt         TIMESTAMP(6)  NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_dt         TIMESTAMP(6)  NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_collection_task_group_task_cron (task_id, cron_expression),
    UNIQUE KEY uk_collection_task_group_collector_job_id (collector_job_id),
    KEY idx_collection_task_group_task_id (task_id),
    CONSTRAINT fk_collection_task_group_task_id
        FOREIGN KEY (task_id) REFERENCES collection_task (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT chk_collection_task_group_active CHECK (active IN (0, 1))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Task 안 주기 그룹 (1분/5분 등)';

CREATE TABLE IF NOT EXISTS collection_task_device (
    id          INT           NOT NULL AUTO_INCREMENT COMMENT '매핑 ID',
    group_id    INT           NOT NULL                COMMENT 'collection_task_group.id',
    device_id   INT           NOT NULL                COMMENT 'devices.id',
    created_dt  TIMESTAMP(6)  NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_dt  TIMESTAMP(6)  NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_collection_task_device_group_device (group_id, device_id),
    KEY idx_collection_task_device_device_id (device_id),
    CONSTRAINT fk_collection_task_device_group_id
        FOREIGN KEY (group_id) REFERENCES collection_task_group (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_collection_task_device_device_id
        FOREIGN KEY (device_id) REFERENCES devices (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='주기 그룹에 속한 장비. 한 Task 안에서 device는 그룹 1개만';
