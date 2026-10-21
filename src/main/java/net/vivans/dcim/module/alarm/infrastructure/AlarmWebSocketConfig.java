package net.vivans.dcim.module.alarm.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.Map;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class AlarmWebSocketConfig implements WebSocketConfigurer {
    private final AlarmWebSocketHandler handler;
    private final AlarmSocketTickets tickets;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/alarms").addInterceptors(new HandshakeInterceptor() {
            @Override
            public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                           WebSocketHandler webSocketHandler, Map<String, Object> attributes) {
                String value = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams().getFirst("ticket");
                AlarmSocketTickets.Ticket ticket = tickets.consume(value);
                if (ticket == null) {
                    response.setStatusCode(HttpStatus.UNAUTHORIZED);
                    return false;
                }
                attributes.put("alarmAdmin", ticket.admin());
                attributes.put("alarmUsername", ticket.username());
                return true;
            }

            @Override
            public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                       WebSocketHandler webSocketHandler, Exception exception) {}
        });
    }
}
