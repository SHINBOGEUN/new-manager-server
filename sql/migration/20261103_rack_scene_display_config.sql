CREATE TABLE IF NOT EXISTS rack_scene_display_config (
  rack_location_code VARCHAR(10) NOT NULL,
  display_json LONGTEXT NOT NULL,
  updated_dt TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (rack_location_code),
  CONSTRAINT fk_rack_scene_display_location FOREIGN KEY (rack_location_code) REFERENCES location_node(code) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
