package net.vivans.dcim.module.alarm.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.vivans.dcim.module.alarm.api.AlarmEventResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlarmWebSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper mapper;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        sessions.remove(session.getId());
        if (session.isOpen()) session.close(CloseStatus.SERVER_ERROR);
    }

    public void publish(AlarmEventResponse event) {
        try {
            String payload = mapper.writeValueAsString(Map.of("type", "alarm", "data", event));
            for (WebSocketSession session : sessions.values()) {
                if (!session.isOpen()) {
                    sessions.remove(session.getId());
                    continue;
                }
                if ("ADMIN_ONLY".equals(event.visibility())
                        && !Boolean.TRUE.equals(session.getAttributes().get("alarmAdmin"))) continue;
                try {
                    synchronized (session) {
                        if (session.isOpen()) session.sendMessage(new TextMessage(payload));
                    }
                } catch (IOException exception) {
                    log.debug("Alarm socket send failed: {}", exception.getMessage());
                    sessions.remove(session.getId());
                }
            }
        } catch (Exception exception) {
            log.warn("Alarm notification serialization failed for event {}: {}", event.id(), exception.getMessage());
        }
    }
}
