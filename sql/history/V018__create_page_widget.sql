-- =============================================================================
-- V018: page_widget / page_widget_point
-- =============================================================================
-- 작성일  : 2026-08-25
-- 대상 DB : MariaDB (dcim_new)
-- 엔티티  : net.vivans.dcim.module.device.domain.model.PageWidget
--           net.vivans.dcim.module.device.domain.model.PageWidgetPoint
--           net.vivans.dcim.shared.persistence.BaseEntity
--
-- 적용 방법 (예시):
--   mysql -h HOST -P PORT -u dcim -p dcim_new < sql/history/V018__create_page_widget.sql
--
-- 선행 조건: V003 (common_code), V014 (DEVICE_PAGE 시드)
--
-- 기존 테이블이 있으면 먼저 삭제:
--   DROP TABLE IF EXISTS page_widget_compare;
--   DROP TABLE IF EXISTS page_widget_point;
--   DROP TABLE IF EXISTS page_widget;
--
-- 설계 문서: docs/device/PAGE_WIDGET_API.md
--
-- 역할:
--   - DEVICE_PAGE 자식. 카드에 last/집계/대수를 보여 줄 정의
--   - 시간축 차트는 이 테이블이 아님 (조회 API)
-- =============================================================================

CREATE TABLE IF NOT EXISTS page_widget (
    id                 INT           NOT NULL AUTO_INCREMENT COMMENT '위젯 ID',
    page_code_id       INT           NOT NULL                COMMENT 'common_code.id (DEVICE_PAGE)',
    name               VARCHAR(100)  NOT NULL                COMMENT '위젯 표시명',
    enabled            TINYINT(1)    NOT NULL DEFAULT 1      COMMENT '사용 여부',
    query_kind         VARCHAR(16)   NOT NULL                COMMENT 'last | aggregate | count',
    op                 VARCHAR(16)   NULL                    COMMENT 'aggregate only: delta_sum | weighted_avg | divide',
    group_by           VARCHAR(16)   NULL                    COMMENT 'device | point | location',
    weight_point       VARCHAR(100)  NULL                    COMMENT 'weighted_avg 가중치 포인트',
    numerator_point    VARCHAR(100)  NULL                    COMMENT 'divide 분자',
    denominator_point  VARCHAR(100)  NULL                    COMMENT 'divide 분모',
    created_dt         TIMESTAMP(6)  NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 시각',
    updated_dt         TIMESTAMP(6)  NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 시각',
    PRIMARY KEY (id),
    UNIQUE KEY uk_page_widget_page_name (page_code_id, name),
    KEY idx_page_widget_page_code_id (page_code_id),
    CONSTRAINT fk_page_widget_page_code_id
        FOREIGN KEY (page_code_id) REFERENCES common_code (id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT chk_page_widget_enabled CHECK (enabled IN (0, 1)),
    CONSTRAINT chk_page_widget_query_kind CHECK (
        query_kind IN ('last', 'aggregate', 'count')
    ),
    CONSTRAINT chk_page_widget_op CHECK (
        op IS NULL OR op IN ('delta_sum', 'weighted_avg', 'divide')
    ),
    CONSTRAINT chk_page_widget_group_by CHECK (
        group_by IS NULL OR group_by IN ('device', 'point', 'location')
    )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='페이지 위젯 카드 정의 (DEVICE_PAGE 자식)';

CREATE TABLE IF NOT EXISTS page_widget_point (
    id          INT          NOT NULL AUTO_INCREMENT COMMENT '매핑 ID',
    widget_id   INT          NOT NULL                COMMENT 'page_widget.id',
    point_name  VARCHAR(100) NOT NULL                COMMENT 'Influx point_name',
    PRIMARY KEY (id),
    UNIQUE KEY uk_page_widget_point_widget_name (widget_id, point_name),
    KEY idx_page_widget_point_widget_id (widget_id),
    CONSTRAINT fk_page_widget_point_widget_id
        FOREIGN KEY (widget_id) REFERENCES page_widget (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='페이지 위젯 조회 포인트 (1:N)';
