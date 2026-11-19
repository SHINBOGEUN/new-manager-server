-- Policy expansion only. Existing settings are never overwritten.
INSERT INTO system_setting(setting_key,setting_value,description)
 SELECT 'collection.failure.threshold','3','연속 실패 통신 오류 판정 기준(1~20)' WHERE NOT EXISTS(SELECT 1 FROM system_setting WHERE setting_key='collection.failure.threshold');
INSERT INTO system_setting(setting_key,setting_value,description)
 SELECT 'collection.backoff.max-seconds','600','실패 장비 백오프 최대 간격(0~86400초, 0은 해제)' WHERE NOT EXISTS(SELECT 1 FROM system_setting WHERE setting_key='collection.backoff.max-seconds');
