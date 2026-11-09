-- Keep existing administrator-managed models unchanged.
INSERT IGNORE INTO `scene_model`
  (`model_key`, `model_name`, `category`, `builtin_file`, `file_name`, `width_m`, `height_m`, `depth_m`)
VALUES
  ('SR_ACCURA_2350_1P', 'ACCURA 2350 단상', '센서', 'accura_2350_1p.glb', 'accura_2350_1p.glb', .045, .105, .045),
  ('SR_ACCURA_2350_3P', 'ACCURA 2350 3상', '센서', 'accura_2350_3p.glb', 'accura_2350_3p.glb', .060, .105, .045);
