-- Add examples without changing models managed by administrators.
INSERT IGNORE INTO `scene_model` (`model_key`, `model_name`, `category`, `builtin_file`, `file_name`, `width_m`, `height_m`, `depth_m`, `rack_installable`) VALUES
('SR_DRAGINO_TH', 'Dragino 온습도 센서', '센서', 'dragino_temperature_humidity.glb', 'dragino_temperature_humidity.glb', .068, .12, .027, 1),
('SR_DRAGINO_DOOR', 'Dragino 도어 센서', '센서', 'dragino_door_contact.glb', 'dragino_door_contact.glb', .13, .10, .03, 1);
