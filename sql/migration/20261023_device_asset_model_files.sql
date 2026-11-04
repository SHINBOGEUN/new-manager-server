-- 자산 장비별 2D 도면 및 3D 모델 파일 분류를 지원한다.
ALTER TABLE `device_asset_document`
  ADD COLUMN `file_kind` varchar(16) NOT NULL DEFAULT 'DOCUMENT'
    COMMENT '첨부 유형 (DOCUMENT/ASSET_2D/ASSET_3D)' AFTER `file_size`;
