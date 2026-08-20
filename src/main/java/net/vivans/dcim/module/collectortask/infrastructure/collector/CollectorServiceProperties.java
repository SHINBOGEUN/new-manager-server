package net.vivans.dcim.module.collectortask.infrastructure.collector;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "collector.service")
public class CollectorServiceProperties {

    private String url = "http://localhost:8081";
    private String apiKey = "manager-server";
    private boolean enabled = true;
    private boolean failFast = true;
    private int retryMaxAttempts = 3;
    private long retryDelayMs = 500;
}
