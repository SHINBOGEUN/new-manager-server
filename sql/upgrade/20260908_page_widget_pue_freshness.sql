ALTER TABLE `page_widget_pue`
  ADD COLUMN `freshness_minutes` int(11) NOT NULL DEFAULT 15 COMMENT '마지막 수집값 허용 경과 분' AFTER `range_preset`;
ALTER TABLE `page_widget_pue`
  ADD CONSTRAINT `chk_page_widget_pue_freshness` CHECK (`freshness_minutes` between 1 and 1440);
