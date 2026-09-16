package net.vivans.dcim.module.mqttmonitor.application;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import net.vivans.dcim.module.mqttmonitor.api.dto.MqttMonitorConnectRequest;
import net.vivans.dcim.module.mqttmonitor.api.dto.MqttMonitorMessageResponse;
import net.vivans.dcim.module.mqttmonitor.api.dto.MqttMonitorMessagesResponse;
import net.vivans.dcim.module.mqttmonitor.api.dto.MqttMonitorSessionResponse;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

@Slf4j
@Service
public class MqttMonitorService {

    private static final int MAX_SESSIONS = 10;
    private static final int MAX_MESSAGES = 500;
    private static final Duration SESSION_TTL = Duration.ofMinutes(10);

    private final Map<String, MonitorSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, Set<Consumer<MqttMonitorMessageResponse>>> messageListeners = new ConcurrentHashMap<>();

    public synchronized MqttMonitorSessionResponse connect(String sessionId, MqttMonitorConnectRequest request) {
        validateSessionId(sessionId);
        disconnect(sessionId);
        if (sessions.size() >= MAX_SESSIONS) {
            throw new IllegalArgumentException("MQTT monitor session limit reached. Disconnect an existing monitor first.");
        }

        MonitorSession session = new MonitorSession(sessionId, request.host(), request.port(), request.topic());
        sessions.put(sessionId, session);
        try {
            MqttClient client = new MqttClient(
                    "tcp://" + request.host() + ":" + request.port(),
                    "manager-monitor-" + UUID.randomUUID(),
                    new MemoryPersistence()
            );
            session.client = client;
            client.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean reconnect, String serverUri) {
                    session.connectionState = "CONNECTED";
                    session.errorMessage = null;
                    session.connectedAt = Instant.now();
                    if (reconnect) {
                        subscribe(client, session);
                    }
                }

                @Override
                public void connectionLost(Throwable cause) {
                    session.connectionState = "RECONNECTING";
                    session.errorMessage = cause == null ? "MQTT connection lost" : cause.getMessage();
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    session.addMessage(topic, message);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                }
            });

            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            options.setConnectionTimeout(8);
            client.connect(options);
            subscribe(client, session);
            session.connectionState = "CONNECTED";
            session.connectedAt = Instant.now();
            log.info("[MQTT_MONITOR_CONNECT] sessionId={} broker={}:{} topic={}",
                    sessionId, request.host(), request.port(), request.topic());
        } catch (Exception exception) {
            session.connectionState = "ERROR";
            session.errorMessage = readableMessage(exception);
            closeClient(session);
            log.warn("[MQTT_MONITOR_CONNECT_ERROR] sessionId={} broker={}:{} message={}",
                    sessionId, request.host(), request.port(), session.errorMessage);
        }
        return session.toResponse();
    }

    public MqttMonitorMessagesResponse getMessages(String sessionId, long afterId) {
        MonitorSession session = getSession(sessionId);
        return new MqttMonitorMessagesResponse(session.toResponse(), session.messagesAfter(afterId));
    }

    public synchronized void disconnect(String sessionId) {
        MonitorSession session = sessions.remove(sessionId);
        if (session == null) {
            return;
        }
        closeClient(session);
        messageListeners.remove(sessionId);
        log.info("[MQTT_MONITOR_DISCONNECT] sessionId={}", sessionId);
    }

    public void addMessageListener(String sessionId, Consumer<MqttMonitorMessageResponse> listener) {
        getSession(sessionId);
        messageListeners.computeIfAbsent(sessionId, ignored -> new CopyOnWriteArraySet<>()).add(listener);
    }

    public void removeMessageListener(String sessionId, Consumer<MqttMonitorMessageResponse> listener) {
        Set<Consumer<MqttMonitorMessageResponse>> listeners = messageListeners.get(sessionId);
        if (listeners == null) {
            return;
        }
        listeners.remove(listener);
        if (listeners.isEmpty()) {
            messageListeners.remove(sessionId, listeners);
        }
    }

    public boolean validateWebSocketTicket(String sessionId, String ticket) {
        MonitorSession session = sessions.get(sessionId);
        return session != null && session.webSocketTicket.equals(ticket);
    }

    @Scheduled(fixedDelay = 60_000)
    public void expireIdleSessions() {
        Instant threshold = Instant.now().minus(SESSION_TTL);
        sessions.values().stream()
                .filter(session -> session.lastAccessed.isBefore(threshold))
                .filter(session -> messageListeners.getOrDefault(session.sessionId, Set.of()).isEmpty())
                .map(session -> session.sessionId)
                .toList()
                .forEach(this::disconnect);
    }

    @PreDestroy
    public void closeAll() {
        new ArrayList<>(sessions.keySet()).forEach(this::disconnect);
    }

    private MonitorSession getSession(String sessionId) {
        validateSessionId(sessionId);
        MonitorSession session = sessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("MQTT monitor session not found. Connect first.");
        }
        session.lastAccessed = Instant.now();
        return session;
    }

    private void subscribe(MqttClient client, MonitorSession session) {
        try {
            client.subscribe(session.topic, 0);
        } catch (MqttException exception) {
            session.connectionState = "ERROR";
            session.errorMessage = readableMessage(exception);
            log.warn("[MQTT_MONITOR_SUBSCRIBE_ERROR] sessionId={} topic={} message={}",
                    session.sessionId, session.topic, session.errorMessage);
        }
    }

    private void closeClient(MonitorSession session) {
        MqttClient client = session.client;
        session.client = null;
        if (client == null) {
            return;
        }
        try {
            if (client.isConnected()) {
                client.disconnect();
            }
            client.close();
        } catch (MqttException exception) {
            log.debug("MQTT monitor close failed: {}", readableMessage(exception));
        }
    }

    private void validateSessionId(String sessionId) {
        if (sessionId == null || !sessionId.matches("[A-Za-z0-9-]{8,64}")) {
            throw new IllegalArgumentException("Invalid MQTT monitor session id");
        }
    }

    private String readableMessage(Exception exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
    }

    private final class MonitorSession {
        private final String sessionId;
        private final String host;
        private final int port;
        private final String topic;
        private final Deque<MqttMonitorMessageResponse> messages = new ArrayDeque<>();
        private volatile MqttClient client;
        private volatile String connectionState = "CONNECTING";
        private volatile String errorMessage;
        private volatile Instant connectedAt;
        private volatile Instant lastAccessed = Instant.now();
        private final String webSocketTicket = UUID.randomUUID().toString();
        private long latestMessageId;

        private MonitorSession(String sessionId, String host, int port, String topic) {
            this.sessionId = sessionId;
            this.host = host;
            this.port = port;
            this.topic = topic;
        }

        private synchronized MqttMonitorMessageResponse addMessage(String topic, MqttMessage message) {
            latestMessageId++;
            MqttMonitorMessageResponse response = new MqttMonitorMessageResponse(
                    latestMessageId,
                    Instant.now(),
                    topic,
                    new String(message.getPayload(), StandardCharsets.UTF_8),
                    message.getQos(),
                    message.isRetained()
            );
            messages.addLast(response);
            while (messages.size() > MAX_MESSAGES) {
                messages.removeFirst();
            }
            notifyListeners(sessionId, response);
            return response;
        }

        private synchronized List<MqttMonitorMessageResponse> messagesAfter(long afterId) {
            return messages.stream().filter(message -> message.id() > afterId).toList();
        }

        private synchronized MqttMonitorSessionResponse toResponse() {
            return new MqttMonitorSessionResponse(
                    sessionId, host, port, topic, connectionState, errorMessage, webSocketTicket,
                    connectedAt, latestMessageId, messages.size()
            );
        }
    }

    private void notifyListeners(String sessionId, MqttMonitorMessageResponse message) {
        Set<Consumer<MqttMonitorMessageResponse>> listeners = messageListeners.get(sessionId);
        if (listeners == null) {
            return;
        }
        listeners.forEach(listener -> {
            try {
                listener.accept(message);
            } catch (Exception exception) {
                log.debug("MQTT monitor WebSocket delivery failed: {}", readableMessage(exception));
            }
        });
    }
}
