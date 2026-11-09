ALTER TABLE `device_page_scene_item` ADD COLUMN IF NOT EXISTS `panel_layout_initialized` tinyint(1) NOT NULL DEFAULT 0;
