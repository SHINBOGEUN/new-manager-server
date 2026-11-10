-- 배포 시 매번 다시 실행되므로(.github/workflows/deploy.yml) 재실행해도 안전하게 IF NOT EXISTS를 쓴다.
ALTER TABLE page_widget
    ADD COLUMN IF NOT EXISTS trend_range_preset VARCHAR(24) NOT NULL DEFAULT 'last_3d',
    ADD COLUMN IF NOT EXISTS trend_window VARCHAR(8) NOT NULL DEFAULT '15m';
