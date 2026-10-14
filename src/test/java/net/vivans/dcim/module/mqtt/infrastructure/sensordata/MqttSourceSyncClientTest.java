package net.vivans.dcim.module.mqtt.infrastructure.sensordata;

import net.vivans.dcim.module.lora.infrastructure.sensordata.SensorDataServiceProperties;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.http.HttpStatus;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class MqttSourceSyncClientTest {
    @Test
    void usesCommonSourceRefreshUrl() {
        var builder = RestClient.builder().baseUrl("http://sensor");
        var server = MockRestServiceServer.bindTo(builder).build();
        var properties = new SensorDataServiceProperties();
        properties.setEnabled(true);
        var client = new MqttSourceSyncClient(builder.build(), properties);
        server.expect(requestTo("http://sensor/api/internal/mqtt/sources/refresh"))
                .andExpect(header("X-Config-Reason", "updated:7")).andRespond(withNoContent());
        client.requestRefresh("updated:7");
        server.verify();
    }

    @Test
    void fallsBackToLegacyOnlyWhenCommonRouteDoesNotExist() {
        var builder = RestClient.builder().baseUrl("http://sensor");
        var server = MockRestServiceServer.bindTo(builder).build();
        var properties = new SensorDataServiceProperties();
        properties.setEnabled(true);
        var client = new MqttSourceSyncClient(builder.build(), properties);
        server.expect(requestTo("http://sensor/api/internal/mqtt/sources/refresh"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
        server.expect(requestTo("http://sensor/api/internal/lora/sources/refresh"))
                .andRespond(withNoContent());
        client.requestRefresh("created:7");
        server.verify();
    }
}
