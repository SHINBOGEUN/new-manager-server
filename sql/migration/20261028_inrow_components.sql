-- Existing catalog: separate the in-row frame and distribution panel.
-- Preserve administrator-uploaded replacements and custom dimensions.
UPDATE `scene_model`
SET `model_name` = '인로우 프레임',
    `builtin_file` = 'in_row_frame.glb',
    `file_name` = 'in_row_frame.glb',
    `width_m` = 1.05,
    `height_m` = 1.05,
    `depth_m` = 1.45
WHERE `model_key` = 'SR_INROW_MOLD'
  AND `builtin_file` = 'in_row_mold.glb'
  AND `storage_key` IS NULL
  AND `width_m` = 2.8 AND `height_m` = 2.2 AND `depth_m` = 1.6;

INSERT IGNORE INTO `scene_model`
  (`model_key`, `model_name`, `builtin_file`, `file_name`, `width_m`, `height_m`, `depth_m`)
VALUES ('SR_INROW_PANEL', '인로우 분전반', 'in_row_distribution_panel.glb', 'in_row_distribution_panel.glb', 1, 1.77, .4);
