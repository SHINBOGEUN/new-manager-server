package net.vivans.dcim.module.lora.infrastructure.sensordata;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "sensor-data.service")
public class SensorDataServiceProperties {
    private String url = "http://localhost:8082";
    private String apiKey = "manager-server";
    private boolean enabled = true;
}
