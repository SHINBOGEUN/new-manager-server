package net.vivans.dcim.module.lora.infrastructure.sensordata;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(SensorDataServiceProperties.class)
public class SensorDataClientConfig {
    @Bean
    @Qualifier("sensorDataRestClient")
    RestClient sensorDataRestClient(SensorDataServiceProperties properties) {
        return RestClient.builder().baseUrl(properties.getUrl())
                .defaultHeader("X-Api-Key", properties.getApiKey()).build();
    }
}
