package net.vivans.dcim.module.mqttmonitor.infrastructure;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.shared.security.SecurityConfig;
import org.springframework.beans.factory.annotation.Value;
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

    /** HTTP CORS와 같은 Origin 목록(CORS_ALLOWED_ORIGINS). 접속 인증은 1회용 ticket이 담당한다. */
    @Value("${security.cors.allowed-origins:*}")
    private String allowedOrigins;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(mqttMonitorWebSocketHandler, "/ws/mqtt-monitor")
                .addInterceptors(mqttMonitorWebSocketInterceptor)
                .setAllowedOriginPatterns(SecurityConfig.parseOrigins(allowedOrigins).toArray(String[]::new));
    }
}
