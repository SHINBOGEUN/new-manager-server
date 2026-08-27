-- =============================================================================
-- V018: page_widget / page_widget_point / page_widget_device / page_widget_layout
--       + device_page 제거
-- =============================================================================
-- 작성일  : 2026-08-26
-- 대상 DB : MariaDB (dcim_new)
-- 엔티티  : net.vivans.dcim.module.device.domain.model.PageWidget
--           net.vivans.dcim.module.device.domain.model.PageWidgetPoint
--           net.vivans.dcim.module.device.domain.model.PageWidgetDevice
--           net.vivans.dcim.module.device.domain.model.PageWidgetLayout
--           net.vivans.dcim.shared.persistence.BaseEntity
--
-- 적용 방법 (예시):
--   mysql -h HOST -P PORT -u dcim -p dcim_new < sql/history/V018__create_page_widget.sql
--
-- 선행 조건: V003 (common_code), V007 (devices), V014 (DEVICE_PAGE 시드)
--
-- 기존 테이블이 있으면 먼저 삭제:
--   DROP TABLE IF EXISTS page_widget_layout;
--   DROP TABLE IF EXISTS page_widget_device;
--   DROP TABLE IF EXISTS page_widget_point;
--   DROP TABLE IF EXISTS page_widget;
--   DROP TABLE IF EXISTS page_widget_compare;
--
-- 설계 문서: docs/device/PAGE_WIDGET_API.md
--
-- 역할:
--   - DEVICE_PAGE 자식. 카드에 last/집계/대수를 보여 줄 정의
--   - 카드 데이터 범위는 page_widget → page_widget_device (+ point)
--   - 2D 그리드 배치는 page_widget_layout (1:1)
--   - device_page(V013)는 중복 매핑이므로 제거 (DEVICE_PAGE 코드는 유지)
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1) page_widget
-- ---------------------------------------------------------------------------
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

-- ---------------------------------------------------------------------------
-- 2) page_widget_point (1:N)
-- ---------------------------------------------------------------------------
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

-- ---------------------------------------------------------------------------
-- 3) page_widget_device (1:N)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS page_widget_device (
    id         INT NOT NULL AUTO_INCREMENT COMMENT '매핑 ID',
    widget_id  INT NOT NULL                COMMENT 'page_widget.id',
    device_id  INT NOT NULL                COMMENT 'devices.id',
    PRIMARY KEY (id),
    UNIQUE KEY uk_page_widget_device_widget_device (widget_id, device_id),
    KEY idx_page_widget_device_widget_id (widget_id),
    KEY idx_page_widget_device_device_id (device_id),
    CONSTRAINT fk_page_widget_device_widget_id
        FOREIGN KEY (widget_id) REFERENCES page_widget (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_page_widget_device_device_id
        FOREIGN KEY (device_id) REFERENCES devices (id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='페이지 위젯 조회 장비 (1:N)';

-- ---------------------------------------------------------------------------
-- 4) page_widget_layout (1:1, 2D 그리드)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS page_widget_layout (
    widget_id  INT NOT NULL COMMENT 'page_widget.id (PK, 1:1)',
    grid_x     INT NOT NULL COMMENT '그리드 X',
    grid_y     INT NOT NULL COMMENT '그리드 Y',
    w          INT NOT NULL COMMENT '가로 칸 수',
    h          INT NOT NULL COMMENT '세로 칸 수',
    created_dt TIMESTAMP(6) NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 시각',
    updated_dt TIMESTAMP(6) NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 시각',
    PRIMARY KEY (widget_id),
    CONSTRAINT fk_page_widget_layout_widget_id
        FOREIGN KEY (widget_id) REFERENCES page_widget (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT chk_page_widget_layout_grid_x CHECK (grid_x >= 0),
    CONSTRAINT chk_page_widget_layout_grid_y CHECK (grid_y >= 0),
    CONSTRAINT chk_page_widget_layout_w CHECK (w >= 1),
    CONSTRAINT chk_page_widget_layout_h CHECK (h >= 1)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='페이지 위젯 UI 그리드 배치 2D (1:1)';

-- ---------------------------------------------------------------------------
-- 5) device_page 제거 (페이지 범위는 page_widget_device로 대체)
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS device_page;
