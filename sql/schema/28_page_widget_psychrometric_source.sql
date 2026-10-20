CREATE TABLE page_widget_psychrometric_source (
    id INT NOT NULL AUTO_INCREMENT COMMENT '심리도 측정 소스 ID',
    widget_id INT NOT NULL COMMENT '심리도 위젯 ID',
    device_id INT NOT NULL COMMENT '측정 소스 장비 ID',
    role VARCHAR(16) NOT NULL COMMENT '심리도 입력 역할 (예: 온도/습도)',
    point_name VARCHAR(100) NOT NULL COMMENT '장비에서 조회할 측정 포인트 이름',
    created_dt DATETIME(6) NULL COMMENT '소스 등록 시각',
    updated_dt DATETIME(6) NULL COMMENT '소스 수정 시각',
    PRIMARY KEY (id),
    CONSTRAINT uk_page_widget_psych_source UNIQUE (widget_id, device_id, point_name),
    CONSTRAINT fk_page_widget_psych_source_widget
        FOREIGN KEY (widget_id) REFERENCES page_widget_psychrometric (widget_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_page_widget_psych_source_device
        FOREIGN KEY (device_id) REFERENCES devices (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='심리도 위젯별 장비 측정 소스';
