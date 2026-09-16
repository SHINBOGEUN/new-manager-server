package net.vivans.dcim.module.operations.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "operations.health")
public class OperationsHealthProperties {

    private String sensorDataUrl = "http://localhost:8082";
    private boolean sensorDataEnabled = true;
    private boolean mqttEnabled = true;
    private String mqttHost = "localhost";
    private int mqttPort = 1883;
    private int timeoutMillis = 2000;
}
