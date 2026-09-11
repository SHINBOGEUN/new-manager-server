CREATE TABLE page_widget_psychrometric (
    widget_id INT NOT NULL,
    PRIMARY KEY (widget_id),
    CONSTRAINT fk_page_widget_psychrometric_widget
        FOREIGN KEY (widget_id) REFERENCES page_widget (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
