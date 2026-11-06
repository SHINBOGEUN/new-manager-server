-- 장비와 도면 설비의 연결·해제 이력 (기존 배치 이력과 별도)
CREATE TABLE IF NOT EXISTS scene_device_connection_history (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  device_id INT NOT NULL,
  page_code VARCHAR(100) NOT NULL,
  previous_host VARCHAR(255) NULL,
  current_host VARCHAR(255) NULL,
  actor_name VARCHAR(255) NULL,
  changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_scene_connection_device_time (device_id, changed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
