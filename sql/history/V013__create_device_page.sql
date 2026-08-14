-- =============================================================================
-- V013: device_page 테이블 생성 (device 모듈 — 장비↔노출 페이지 매핑)
-- =============================================================================
-- 작성일  : 2026-08-14
-- 대상 DB : MariaDB (dcim_new)
-- 엔티티  : net.vivans.dcim.module.device.domain.model.DevicePage
--           net.vivans.dcim.shared.persistence.BaseEntity
--
-- 적용 방법 (예시):
--   mysql -h HOST -P PORT -u dcim -p dcim_new < sql/history/V013__create_device_page.sql
--
-- 선행 조건: V003 (common_code), V007 (devices)
--
-- 설계 문서:
--   docs/device/DEVICE_PAGE_API.md
--   docs/BACKLOG.md — Device ↔ Page
--
-- 역할:
--   - Environment / Cooling / Analysis / Power 등 화면에 올릴 장비를 DB로 지정
--   - 구 manager codeKey·zone ID·analysisYn 하드코딩 대체
--   - point가 아니라 **device 단위** N:M 매핑
--
-- 비즈니스 규칙 (애플리케이션에서 검증):
--   - page_code_id — DEVICE_PAGE 그룹 common_code만 허용
--   - (device_id, page_code_id) UK
--   - devices 삭제 시 CASCADE
-- =============================================================================

CREATE TABLE IF NOT EXISTS device_page (
    id                 INT           NOT NULL AUTO_INCREMENT COMMENT '매핑 ID',
    device_id          INT           NOT NULL                COMMENT 'devices.id (FK)',
    page_code_id       INT           NOT NULL                COMMENT 'common_code.id (DEVICE_PAGE)',
    created_dt         TIMESTAMP(6)  NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 시각',
    updated_dt         TIMESTAMP(6)  NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 시각',
    PRIMARY KEY (id),
    UNIQUE KEY uk_device_page_device_page_code (device_id, page_code_id),
    KEY idx_device_page_device_id (device_id),
    KEY idx_device_page_page_code_id (page_code_id),
    CONSTRAINT fk_device_page_device_id
        FOREIGN KEY (device_id) REFERENCES devices (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_device_page_page_code_id
        FOREIGN KEY (page_code_id) REFERENCES common_code (id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='장비↔노출 페이지 매핑 (DEVICE_PAGE common_code)';
