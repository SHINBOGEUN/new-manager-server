ALTER TABLE page_widget
    ADD COLUMN trend_range_preset VARCHAR(24) NOT NULL DEFAULT 'last_3d',
    ADD COLUMN trend_window VARCHAR(8) NOT NULL DEFAULT '15m';
