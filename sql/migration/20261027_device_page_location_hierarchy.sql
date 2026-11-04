-- 기존 페이지 도면에도 위치 노드 하위 랙을 최초 1회 자동 배치한다.
-- 배치 목록을 저장하면 initialized=1이 되어 이후 사용자가 삭제한 랙은 다시 생성되지 않는다.
ALTER TABLE `device_page_display_config`
  ADD COLUMN `location_hierarchy_initialized` tinyint(1) NOT NULL DEFAULT 0
    COMMENT 'Location hierarchy initialization status';
