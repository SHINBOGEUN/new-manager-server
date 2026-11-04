-- Register examples without replacing administrator-managed models.
INSERT IGNORE INTO `scene_model`
 (`model_key`, `model_name`, `category`, `builtin_file`, `file_name`, `width_m`, `height_m`, `depth_m`)
VALUES
 ('SR_PDU_RACK', '랙 장착형 PDU', '전력 설비', 'pdu_rack_horizontal.glb', 'pdu_rack_horizontal.glb', .482, .11, .24),
 ('SR_PDU_VERTICAL', '세로형 PDU', '전력 설비', 'pdu_rack_vertical.glb', 'pdu_rack_vertical.glb', .1, 1.6, .1),
 ('SR_GPU_SERVER', 'GPU 서버 (4U 예시)', '랙·서버', 'gpu_server_rack.glb', 'gpu_server_rack.glb', .482, .178, .72);
