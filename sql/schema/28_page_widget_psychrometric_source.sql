CREATE TABLE page_widget_psychrometric_source (
    id INT NOT NULL AUTO_INCREMENT,
    widget_id INT NOT NULL,
    device_id INT NOT NULL,
    role VARCHAR(16) NOT NULL,
    point_name VARCHAR(100) NOT NULL,
    created_dt DATETIME(6) NULL,
    updated_dt DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_page_widget_psych_source UNIQUE (widget_id, device_id, point_name),
    CONSTRAINT fk_page_widget_psych_source_widget
        FOREIGN KEY (widget_id) REFERENCES page_widget_psychrometric (widget_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_page_widget_psych_source_device
        FOREIGN KEY (device_id) REFERENCES devices (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
