-- =============================================================================
-- V018: page_widget (core) + kind별 1:1 확장 + point/device/model/layout
--       + device_page 제거
-- =============================================================================
-- 작성일  : 2026-08-27
-- 대상 DB : MariaDB (dcim_new)
--
-- page_widget 는 공통 메타만. queryKind별 옵션은 1:1 확장 테이블:
--   aggregate → page_widget_aggregate
--   count     → page_widget_count
--   chart     → page_widget_chart
--   last      → (확장 없음, group_by 는 공통 표시 옵션)
--
-- 선행: V003 (common_code), V007 (devices), V014 (DEVICE_PAGE 시드)
-- 설계: docs/device/PAGE_WIDGET_API.md
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1) page_widget (core)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS page_widget (
    id            INT          NOT NULL AUTO_INCREMENT COMMENT '위젯 ID',
    page_code_id  INT          NOT NULL                COMMENT 'common_code.id (DEVICE_PAGE)',
    name          VARCHAR(100) NOT NULL                COMMENT '위젯 표시명',
    enabled       TINYINT(1)   NOT NULL DEFAULT 1      COMMENT '사용 여부',
    query_kind    VARCHAR(16)  NOT NULL                COMMENT 'last | aggregate | count | chart',
    group_by      VARCHAR(16)  NULL                    COMMENT '표시 그룹: device | point | location',
    created_dt    TIMESTAMP(6) NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 시각',
    updated_dt    TIMESTAMP(6) NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 시각',
    PRIMARY KEY (id),
    UNIQUE KEY uk_page_widget_page_name (page_code_id, name),
    KEY idx_page_widget_page_code_id (page_code_id),
    CONSTRAINT fk_page_widget_page_code_id
        FOREIGN KEY (page_code_id) REFERENCES common_code (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_page_widget_enabled CHECK (enabled IN (0, 1)),
    CONSTRAINT chk_page_widget_query_kind CHECK (
        query_kind IN ('last', 'aggregate', 'count', 'chart')
    ),
    CONSTRAINT chk_page_widget_group_by CHECK (
        group_by IS NULL OR group_by IN ('device', 'point', 'location')
    )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='페이지 위젯 카드 정의 (DEVICE_PAGE 자식, core)';

-- ---------------------------------------------------------------------------
-- 2) page_widget_aggregate (1:1, query_kind=aggregate)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS page_widget_aggregate (
    widget_id           INT          NOT NULL COMMENT 'page_widget.id (PK, 1:1)',
    op                  VARCHAR(16)  NOT NULL COMMENT 'delta_sum | weighted_avg | divide',
    weight_point        VARCHAR(100) NULL     COMMENT 'weighted_avg 가중치 포인트',
    numerator_point     VARCHAR(100) NULL     COMMENT 'divide 분자',
    denominator_point   VARCHAR(100) NULL     COMMENT 'divide 분모',
    created_dt          TIMESTAMP(6) NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_dt          TIMESTAMP(6) NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (widget_id),
    CONSTRAINT fk_page_widget_aggregate_widget_id
        FOREIGN KEY (widget_id) REFERENCES page_widget (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_page_widget_aggregate_op CHECK (
        op IN ('delta_sum', 'weighted_avg', 'divide')
    )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='aggregate 위젯 옵션 (1:1)';

-- ---------------------------------------------------------------------------
-- 3) page_widget_count (1:1, query_kind=count)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS page_widget_count (
    widget_id       INT         NOT NULL COMMENT 'page_widget.id (PK, 1:1)',
    count_mode      VARCHAR(16) NOT NULL COMMENT 'total | by_model | model',
    count_model_id  INT         NULL     COMMENT 'count_mode=model 일 때 model_id',
    created_dt      TIMESTAMP(6) NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_dt      TIMESTAMP(6) NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (widget_id),
    KEY idx_page_widget_count_model_id (count_model_id),
    CONSTRAINT fk_page_widget_count_widget_id
        FOREIGN KEY (widget_id) REFERENCES page_widget (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_page_widget_count_model_id
        FOREIGN KEY (count_model_id) REFERENCES device_model (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_page_widget_count_mode CHECK (
        count_mode IN ('total', 'by_model', 'model')
    )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='count 위젯 옵션 (1:1)';

-- ---------------------------------------------------------------------------
-- 4) page_widget_chart (1:1, query_kind=chart)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS page_widget_chart (
    widget_id           INT         NOT NULL COMMENT 'page_widget.id (PK, 1:1)',
    chart_scope         VARCHAR(16) NOT NULL COMMENT 'devices | models',
    chart_series_mode   VARCHAR(16) NOT NULL COMMENT 'per_device | sum | by_phase | by_path',
    chart_range_preset  VARCHAR(16) NOT NULL COMMENT 'last_24h | today | yesterday | last_7d | this_month',
    chart_window        VARCHAR(8)  NOT NULL COMMENT '1m|5m|15m|1h|1d',
    created_dt          TIMESTAMP(6) NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_dt          TIMESTAMP(6) NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (widget_id),
    CONSTRAINT fk_page_widget_chart_widget_id
        FOREIGN KEY (widget_id) REFERENCES page_widget (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_page_widget_chart_scope CHECK (
        chart_scope IN ('devices', 'models')
    ),
    CONSTRAINT chk_page_widget_chart_series_mode CHECK (
        chart_series_mode IN ('per_device', 'sum', 'by_phase', 'by_path')
    ),
    CONSTRAINT chk_page_widget_chart_range_preset CHECK (
        chart_range_preset IN ('last_24h', 'today', 'yesterday', 'last_7d', 'this_month')
    )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='chart 위젯 옵션 (1:1)';

-- ---------------------------------------------------------------------------
-- 5) page_widget_point (1:N)
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
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='페이지 위젯 조회 포인트 (1:N)';

-- ---------------------------------------------------------------------------
-- 6) page_widget_device (1:N)
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
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_page_widget_device_device_id
        FOREIGN KEY (device_id) REFERENCES devices (id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='페이지 위젯 조회 장비 (1:N)';

-- ---------------------------------------------------------------------------
-- 7) page_widget_model (1:N, chart scope=models)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS page_widget_model (
    id         INT NOT NULL AUTO_INCREMENT,
    widget_id  INT NOT NULL COMMENT 'page_widget.id',
    model_id   INT NOT NULL COMMENT 'device_model.id',
    PRIMARY KEY (id),
    UNIQUE KEY uk_page_widget_model_widget_model (widget_id, model_id),
    KEY idx_page_widget_model_widget_id (widget_id),
    KEY idx_page_widget_model_model_id (model_id),
    CONSTRAINT fk_page_widget_model_widget_id
        FOREIGN KEY (widget_id) REFERENCES page_widget (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_page_widget_model_model_id
        FOREIGN KEY (model_id) REFERENCES device_model (id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='chart 위젯 모델 범위 (1:N)';

-- ---------------------------------------------------------------------------
-- 8) page_widget_layout (1:1, 2D 그리드)
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
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_page_widget_layout_grid_x CHECK (grid_x >= 0),
    CONSTRAINT chk_page_widget_layout_grid_y CHECK (grid_y >= 0),
    CONSTRAINT chk_page_widget_layout_w CHECK (w >= 1),
    CONSTRAINT chk_page_widget_layout_h CHECK (h >= 1)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='페이지 위젯 UI 그리드 배치 2D (1:1)';

-- ---------------------------------------------------------------------------
-- 9) 구 device_page 테이블 정리 (과거 환경용. 신규 배포에는 테이블 없음)
--    장비 범위는 page_widget_device 로 대체.
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS device_page;
