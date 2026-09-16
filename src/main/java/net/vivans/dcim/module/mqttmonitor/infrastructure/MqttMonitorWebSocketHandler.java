package net.vivans.dcim.module.mqttmonitor.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.vivans.dcim.module.mqttmonitor.api.dto.MqttMonitorMessageResponse;
import net.vivans.dcim.module.mqttmonitor.application.MqttMonitorService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class MqttMonitorWebSocketHandler extends TextWebSocketHandler {

    private final MqttMonitorService mqttMonitorService;
    private final ObjectMapper objectMapper;
    private final Map<String, Subscription> subscriptions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String monitorSessionId = (String) session.getAttributes().get("mqttMonitorSessionId");
        Consumer<MqttMonitorMessageResponse> listener = message -> sendMessage(session, message);
        subscriptions.put(session.getId(), new Subscription(monitorSessionId, listener));
        mqttMonitorService.addMessageListener(monitorSessionId, listener);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        removeSubscription(session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        removeSubscription(session.getId());
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    private void sendMessage(WebSocketSession session, MqttMonitorMessageResponse message) {
        if (!session.isOpen()) {
            removeSubscription(session.getId());
            return;
        }
        try {
            String payload = objectMapper.writeValueAsString(Map.of("type", "message", "data", message));
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(payload));
                }
            }
        } catch (IOException exception) {
            log.debug("MQTT monitor WebSocket send failed: {}", exception.getMessage());
            removeSubscription(session.getId());
        }
    }

    private void removeSubscription(String webSocketSessionId) {
        Subscription subscription = subscriptions.remove(webSocketSessionId);
        if (subscription != null) {
            mqttMonitorService.removeMessageListener(subscription.monitorSessionId(), subscription.listener());
        }
    }

    private record Subscription(String monitorSessionId, Consumer<MqttMonitorMessageResponse> listener) {
    }
}
