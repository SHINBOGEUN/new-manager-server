-- 기존 DB의 common_code에 임계값 사용 플래그를 추가한다.
-- 기존 코드는 DEFAULT 0(미허용)으로 초기화된다. 임계값에서 사용할 DATA_POINT_TYPE만
-- Ops Console에서 편집해 허용한다. 컬럼이 이미 있으면 다시 실행하지 않는다.
ALTER TABLE `common_code`
  ADD COLUMN `threshold_supported` tinyint(1) NOT NULL DEFAULT 0
  COMMENT 'DATA_POINT_TYPE 임계값 지원 여부';
