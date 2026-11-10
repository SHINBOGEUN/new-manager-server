package net.vivans.dcim.module.lora.infrastructure.sensordata;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "sensor-data.service")
public class SensorDataServiceProperties {
    /** MQTT 수신 기능이 Collector에 통합되어 Collector 주소(COLLECTOR_SERVICE_URL)를 쓴다. */
    private String url;
    private String apiKey = "";
    private boolean enabled = true;
}
