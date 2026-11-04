-- 기존 도면 객체를 보존하면서 랙별 색상과 불필요한 신규 모델 선택만 조정한다.
ALTER TABLE device_page_scene_item ADD COLUMN IF NOT EXISTS rack_color VARCHAR(7) DEFAULT NULL COMMENT '랙 표시 색상 (#RRGGBB)';
UPDATE scene_model SET enabled = 0 WHERE model_key IN ('SR_DLC_RACK', 'SR_INROW_RACK', 'SR_HAC_RACK', 'SR_OCP_RACK', 'SR_ORV3_RACK');
