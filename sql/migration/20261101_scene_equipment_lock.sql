ALTER TABLE `device_page_scene_item`
  ADD COLUMN IF NOT EXISTS `locked` tinyint(1) NOT NULL DEFAULT 0 COMMENT '설비 배치 잠금 여부';
