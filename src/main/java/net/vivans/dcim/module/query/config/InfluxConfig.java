package net.vivans.dcim.module.query.config;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.InfluxDBClientOptions;
import lombok.extern.slf4j.Slf4j;
import net.vivans.dcim.module.query.domain.PointQuery;
import net.vivans.dcim.module.query.infrastructure.influx.DisabledPointQuery;
import net.vivans.dcim.module.query.infrastructure.influx.InfluxPointQuery;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Slf4j
@Configuration
@EnableConfigurationProperties(InfluxProperties.class)
public class InfluxConfig {

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "influx", name = "enabled", havingValue = "true")
    public InfluxDBClient influxDBClient(InfluxProperties properties) {
        log.info(
                "InfluxDB query client: url={} org={} bucket={}",
                properties.getUrl(),
                properties.getOrg(),
                properties.getBucket()
        );
        OkHttpClient.Builder okHttpBuilder = new OkHttpClient.Builder()
                .readTimeout(Duration.ofSeconds(properties.getReadTimeoutSeconds()))
                .connectTimeout(Duration.ofSeconds(properties.getConnectTimeoutSeconds()));
        InfluxDBClientOptions options = InfluxDBClientOptions.builder()
                .url(properties.getUrl())
                .authenticateToken(properties.getToken().toCharArray())
                .org(properties.getOrg())
                .bucket(properties.getBucket())
                .okHttpClient(okHttpBuilder)
                .build();
        return InfluxDBClientFactory.create(options);
    }

    @Bean
    public PointQuery pointQuery(
            ObjectProvider<InfluxDBClient> clientProvider,
            InfluxProperties properties
    ) {
        InfluxDBClient client = clientProvider.getIfAvailable();
        if (!properties.isEnabled() || client == null) {
            log.info("InfluxDB query disabled");
            return new DisabledPointQuery();
        }
        return new InfluxPointQuery(client, properties);
    }
}
