ALTER TABLE device_rack_placement ADD COLUMN IF NOT EXISTS visual_length_ratio DOUBLE DEFAULT NULL COMMENT '측면 장착 3D 모델의 랙 높이 대비 길이';
ALTER TABLE rack_model_placement ADD COLUMN IF NOT EXISTS visual_length_ratio DOUBLE DEFAULT NULL COMMENT '측면 장착 3D 모델의 랙 높이 대비 길이';
