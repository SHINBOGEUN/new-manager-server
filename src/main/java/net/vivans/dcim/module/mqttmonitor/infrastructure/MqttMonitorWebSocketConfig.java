package net.vivans.dcim.module.mqttmonitor.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class MqttMonitorWebSocketConfig implements WebSocketConfigurer {

    private final MqttMonitorWebSocketHandler mqttMonitorWebSocketHandler;
    private final MqttMonitorWebSocketInterceptor mqttMonitorWebSocketInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(mqttMonitorWebSocketHandler, "/ws/mqtt-monitor")
                .addInterceptors(mqttMonitorWebSocketInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
