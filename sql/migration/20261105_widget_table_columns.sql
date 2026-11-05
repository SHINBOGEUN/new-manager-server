-- Table widgets use latest-value sources and persist the selected column names and order.
ALTER TABLE `page_widget` ADD COLUMN IF NOT EXISTS `table_columns_json` TEXT DEFAULT NULL;
