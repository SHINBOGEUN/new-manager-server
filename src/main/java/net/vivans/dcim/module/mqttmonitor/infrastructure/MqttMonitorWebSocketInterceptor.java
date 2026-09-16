package net.vivans.dcim.module.mqttmonitor.infrastructure;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.mqttmonitor.application.MqttMonitorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class MqttMonitorWebSocketInterceptor implements HandshakeInterceptor {

    private final MqttMonitorService mqttMonitorService;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler webSocketHandler,
            Map<String, Object> attributes
    ) {
        var query = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams();
        String sessionId = query.getFirst("sessionId");
        String ticket = query.getFirst("ticket");
        if (!mqttMonitorService.validateWebSocketTicket(sessionId, ticket)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        attributes.put("mqttMonitorSessionId", sessionId);
        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler webSocketHandler,
            Exception exception
    ) {
    }
}
