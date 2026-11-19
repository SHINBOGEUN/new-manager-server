-- Add a chart comparison default without changing existing widgets.
SET @dcim_column_count = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'page_widget_chart' AND COLUMN_NAME = 'compare_mode'
);
SET @dcim_ddl = IF(@dcim_column_count = 0,
  'ALTER TABLE page_widget_chart ADD COLUMN compare_mode VARCHAR(24) NOT NULL DEFAULT ''none''',
  'SELECT 1');
PREPARE dcim_stmt FROM @dcim_ddl;
EXECUTE dcim_stmt;
DEALLOCATE PREPARE dcim_stmt;

UPDATE page_widget_chart SET compare_mode = 'none' WHERE compare_mode IS NULL;
