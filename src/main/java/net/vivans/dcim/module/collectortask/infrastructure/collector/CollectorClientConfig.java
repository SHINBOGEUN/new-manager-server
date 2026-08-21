package net.vivans.dcim.module.collectortask.infrastructure.collector;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(CollectorServiceProperties.class)
public class CollectorClientConfig {

    @Bean
    RestClient collectorRestClient(CollectorServiceProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.getUrl())
                .defaultHeader("X-Api-Key", properties.getApiKey())
                .build();
    }
}
