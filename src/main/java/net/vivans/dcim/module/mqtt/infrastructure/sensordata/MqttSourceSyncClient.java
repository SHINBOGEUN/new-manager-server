package net.vivans.dcim.module.mqtt.infrastructure.sensordata;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import net.vivans.dcim.module.lora.infrastructure.sensordata.SensorDataServiceProperties;

/** Manager 설정 변경을 Collector(MQTT 수신)에 즉시 알린다. 실패해도 Collector(MQTT 수신)의 1분 재조정이 복구한다. */
@Slf4j
@Component
public class MqttSourceSyncClient {
    private final RestClient restClient;
    private final SensorDataServiceProperties properties;

    public MqttSourceSyncClient(@Qualifier("sensorDataRestClient") RestClient restClient,
                                    SensorDataServiceProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public void requestRefresh(String reason) {
        if (!properties.isEnabled()) return;
        try {
            restClient.post().uri("/api/internal/mqtt/sources/refresh")
                    .header("X-Config-Reason", reason).retrieve().toBodilessEntity();
            log.info("[MQTT_SOURCE_SYNC] Collector(MQTT 수신) refresh requested: reason={}", reason);
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound exception) {
            // Collector(MQTT 수신) 전환 전 기존 LoRa 소스 동기화는 계속 지원한다.
            try {
                restClient.post().uri("/api/internal/lora/sources/refresh")
                        .header("X-Config-Reason", reason).retrieve().toBodilessEntity();
                log.info("[MQTT_SOURCE_SYNC] Legacy LoRa refresh requested; AGENT requires Collector(MQTT 수신) upgrade");
            } catch (Exception fallbackException) {
                log.warn("[MQTT_SOURCE_SYNC] Source refresh failed; periodic reconciliation required");
            }
        } catch (Exception exception) {
            log.warn("[LORA_SOURCE_SYNC] Collector(MQTT 수신) refresh request failed; reconciliation will retry: reason={}, message={}",
                    reason, exception.getMessage());
        }
    }

    public void requestConfigRefresh(String reason) {
        if (!properties.isEnabled()) return;
        try {
            restClient.post().uri("/api/internal/lora/config/refresh")
                    .header("X-Config-Reason", reason).retrieve().toBodilessEntity();
            log.info("[LORA_CONFIG_SYNC] Collector(MQTT 수신) config refresh requested: reason={}", reason);
        } catch (Exception exception) {
            log.warn("[LORA_CONFIG_SYNC] Collector(MQTT 수신) config refresh request failed; TTL refresh will retry: reason={}, message={}",
                    reason, exception.getMessage());
        }
    }
}
