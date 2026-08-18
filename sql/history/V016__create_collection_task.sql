-- =============================================================================
-- V016: collection_task 테이블 생성 (collector task 모듈)
-- =============================================================================
-- 작성일  : 2026-08-18
-- 대상 DB : MariaDB (dcim_new)
--
-- 적용 방법 (예시):
--   mysql -h HOST -P PORT -u dcim -p dcim_new < sql/history/V016__create_collection_task.sql
--
-- 선행 조건: V003 (common_code)
--
-- 설계 문서:
--   docs/collection-task/COLLECTION_TASK_DESIGN.md
--
-- 역할:
--   - 수집 task의 이름, 주기(cron), 스크립트, collector 연동 상태를 저장
--   - 수집 대상은 모든 장비이며 페이지/위치별 필터를 두지 않음
--
-- 비즈니스 규칙 (애플리케이션에서 검증):
--   - script_type_id -- PROTOCOL_TYPE 그룹 common_code만 허용
--   - cron_expression -- Spring cron 형식 유효성은 애플리케이션에서 검증
--   - collector_task_id -- new-collector-service 등록 후에만 값 존재
--   - active -- 0=false, 1=true
-- =============================================================================

CREATE TABLE IF NOT EXISTS collection_task (
    id                 CHAR(36)      NOT NULL                COMMENT 'Task ID (UUID 문자열)',
    name               VARCHAR(100)  NOT NULL                COMMENT 'Task 이름',
    cron_expression    VARCHAR(100)  NOT NULL                COMMENT '수집 주기 cron 표현식',
    script_type_id     INT           NOT NULL                COMMENT 'common_code.id (PROTOCOL_TYPE)',
    generated_script   LONGTEXT      NULL                    COMMENT '자동 생성된 JS 스크립트',
    collector_task_id  VARCHAR(100)  NULL                    COMMENT 'new-collector-service 등록 ID',
    active             TINYINT(1)    NOT NULL DEFAULT 1      COMMENT '활성 여부 (0=false, 1=true)',
    created_dt         TIMESTAMP(6)  NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 시각',
    updated_dt         TIMESTAMP(6)  NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 시각',
    PRIMARY KEY (id),
    UNIQUE KEY uk_collection_task_collector_task_id (collector_task_id),
    KEY idx_collection_task_script_type_id (script_type_id),
    KEY idx_collection_task_active (active),
    CONSTRAINT fk_collection_task_script_type_id
        FOREIGN KEY (script_type_id) REFERENCES common_code (id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT chk_collection_task_active
        CHECK (active IN (0, 1))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='수집 task 기본 정보 (전체 장비 대상, 주기, 스크립트 타입, 생성 스크립트, collector 동기화 상태)';
