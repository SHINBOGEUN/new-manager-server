-- 위젯 레이아웃 그리드 버전: 기존 행은 0(레거시 판별), 새/수정 행은 애플리케이션이 48로 저장
ALTER TABLE `page_widget_layout`
  ADD COLUMN `grid_version` int(11) NOT NULL DEFAULT 0
    COMMENT '그리드 기준 버전 (48=48열 기준, 0=레거시 자동 판별)';
