CREATE TABLE page_widget_psychrometric (
    widget_id INT NOT NULL COMMENT 'page_widget.id 및 심리도 위젯 ID',
    PRIMARY KEY (widget_id),
    CONSTRAINT fk_page_widget_psychrometric_widget
        FOREIGN KEY (widget_id) REFERENCES page_widget (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='심리도 위젯의 1:1 설정';
