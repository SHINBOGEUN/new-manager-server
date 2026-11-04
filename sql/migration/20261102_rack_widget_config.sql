CREATE TABLE IF NOT EXISTS rack_widget_config (
  rack_location_code VARCHAR(10) NOT NULL,
  widgets_json LONGTEXT NOT NULL,
  updated_dt TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (rack_location_code),
  CONSTRAINT fk_rack_widget_config_location FOREIGN KEY (rack_location_code) REFERENCES location_node(code) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
