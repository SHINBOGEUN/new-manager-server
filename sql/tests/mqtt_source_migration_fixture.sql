-- 임시 테스트 DB 전용. schema/50,51 적용 후, 이관 전에 실행.
INSERT INTO lora_mqtt_source
  (id,name,source_type,broker_url,topic,credential_key,enabled,config_version)
VALUES (7,'preserve-lora','CHIRPSTACK_LORA','tcp://broker:1883','application/#','chirpstack',1,4);
INSERT INTO lora_mqtt_source_status (source_id,status,message_count,error_count)
VALUES (7,'CONNECTED',123,2);
