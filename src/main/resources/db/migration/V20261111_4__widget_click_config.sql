-- Keep this migration retry-safe when an operator has added the column manually.
SET @dcim_column_count = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'page_widget' AND COLUMN_NAME = 'click_config'
);
SET @dcim_ddl = IF(@dcim_column_count = 0,
  'ALTER TABLE page_widget ADD COLUMN click_config TEXT NULL',
  'SELECT 1');
PREPARE dcim_stmt FROM @dcim_ddl;
EXECUTE dcim_stmt;
DEALLOCATE PREPARE dcim_stmt;

-- Preserve the legacy trend defaults in the new click configuration.
UPDATE page_widget
SET click_config = CASE
  WHEN query_kind IN ('count', 'cctv') THEN JSON_OBJECT('action', 'none')
  ELSE JSON_OBJECT('action', 'chart', 'chartStyle', 'line',
       'rangePreset', COALESCE(trend_range_preset, 'last_3d'),
       'window', COALESCE(trend_window, '15m'))
END
WHERE click_config IS NULL;
