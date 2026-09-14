package net.vivans.dcim.module.operations.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OperationsHealthProperties.class)
public class OperationsHealthConfig {
}
