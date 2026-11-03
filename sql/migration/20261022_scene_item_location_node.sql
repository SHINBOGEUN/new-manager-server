-- 실제 location_node의 RACK을 페이지 도면에 배치하고 좌표를 저장
ALTER TABLE `device_page_scene_item`
  ADD COLUMN `location_node_code` char(10) DEFAULT NULL
    COMMENT '실제 랙 위치 노드 코드 (LOCATION_TYPE=RACK)' AFTER `device_id`,
  ADD KEY `idx_device_page_scene_item_location_node` (`location_node_code`),
  ADD UNIQUE KEY `uk_device_page_scene_item_page_location_type`
    (`page_code_id`, `location_node_code`, `item_type`),
  ADD CONSTRAINT `fk_device_page_scene_item_location_node` FOREIGN KEY (`location_node_code`)
    REFERENCES `location_node` (`code`) ON DELETE RESTRICT ON UPDATE CASCADE;
