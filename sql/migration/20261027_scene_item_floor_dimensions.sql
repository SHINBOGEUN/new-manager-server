-- Preserve fitted floor-tile dimensions so repaired gaps remain correct after reload.
ALTER TABLE `device_page_scene_item`
  ADD COLUMN `size_x` double NOT NULL DEFAULT 4 COMMENT '공간 타일 X 크기(미터, 바닥 타일)' AFTER `rotation_y`,
  ADD COLUMN `size_z` double NOT NULL DEFAULT 4 COMMENT '공간 타일 Z 크기(미터, 바닥 타일)' AFTER `size_x`;
