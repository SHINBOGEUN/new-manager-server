-- 기존 dcim DB의 활성 테이블 중 설명이 비어 있던 테이블에 목적을 기록한다.
-- 데이터와 컬럼 구조를 변경하지 않으며, 테이블 주석만 갱신한다.

ALTER TABLE `calculated_metric`
  COMMENT = 'Collector에서 주기적으로 계산하는 지표 정의';
ALTER TABLE `calculated_metric_source`
  COMMENT = '계산 지표 alias와 장비 측정 포인트의 연결';
ALTER TABLE `device_asset`
  COMMENT = '장비 인스턴스와 분리된 자산 관리 속성';
ALTER TABLE `device_asset_document`
  COMMENT = '장비 자산에 첨부된 문서 메타데이터';
ALTER TABLE `page_widget_aggregate`
  COMMENT = '사용량 또는 전력 집계 위젯 설정';
ALTER TABLE `page_widget_calculated`
  COMMENT = '계산 지표 위젯의 표시 기간 및 신선도 설정';
ALTER TABLE `page_widget_chart`
  COMMENT = '측정 추이 차트 위젯 설정';
ALTER TABLE `page_widget_count`
  COMMENT = '장비 수 위젯 집계 설정';
ALTER TABLE `page_widget_device_group`
  COMMENT = '위젯에서 참조하는 장비 그룹';
ALTER TABLE `page_widget_psychrometric`
  COMMENT = '심리도 위젯의 1:1 설정';
ALTER TABLE `page_widget_psychrometric_source`
  COMMENT = '심리도 위젯별 장비 측정 소스';
