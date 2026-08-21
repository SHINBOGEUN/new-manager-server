package net.vivans.dcim.module.live.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(LiveTelemetryProperties.class)
public class LiveTelemetryConfig {
}
