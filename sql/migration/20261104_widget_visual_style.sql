ALTER TABLE page_widget
  ADD COLUMN visual_style varchar(16) NOT NULL DEFAULT 'classic' COMMENT '위젯 디자인',
  ADD CONSTRAINT chk_page_widget_visual_style CHECK (visual_style IN ('classic','minimal','vivid'));
