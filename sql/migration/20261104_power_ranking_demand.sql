-- Expand existing aggregate options without modifying widget rows. Apply once to each existing database.
ALTER TABLE page_widget_aggregate DROP CONSTRAINT chk_page_widget_aggregate_op,
  ADD CONSTRAINT chk_page_widget_aggregate_op
  CHECK (op IN ('usage', 'power', 'usage_rank', 'power_rank', 'demand'));
