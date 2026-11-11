package net.vivans.dcim.module.collectortask.infrastructure.collector;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "collector.service")
public class CollectorServiceProperties {

    /** 기본값은 application.yml(COLLECTOR_SERVICE_URL)에서만 정한다. */
    private String url;
    private String apiKey = "";
    private boolean enabled = true;
    private boolean failFast = true;
    private int retryMaxAttempts = 3;
    private long retryDelayMs = 500;
}
