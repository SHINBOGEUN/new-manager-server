-- V019: count 위젯 집계 방식 (total | by_model | model)
ALTER TABLE page_widget
    ADD COLUMN count_mode VARCHAR(16) NULL COMMENT 'count only: total | by_model | model' AFTER denominator_point,
    ADD COLUMN count_model_id INT NULL COMMENT 'count_mode=model 일 때 대상 model_id' AFTER count_mode;

ALTER TABLE page_widget
    ADD CONSTRAINT fk_page_widget_count_model_id
        FOREIGN KEY (count_model_id) REFERENCES device_model (id)
        ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE page_widget
    ADD CONSTRAINT chk_page_widget_count_mode CHECK (
        count_mode IS NULL OR count_mode IN ('total', 'by_model', 'model')
    );
