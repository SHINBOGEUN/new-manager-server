package net.vivans.dcim.module.operations.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "operations.health")
public class OperationsHealthProperties {

    private boolean mqttEnabled = true;
    /** 기본값은 application.yml(MQTT_HOST)에서만 정한다. */
    private String mqttHost;
    private int mqttPort = 1883;
    private int timeoutMillis = 2000;
}
