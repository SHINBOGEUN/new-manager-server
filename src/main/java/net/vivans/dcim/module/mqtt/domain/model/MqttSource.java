package net.vivans.dcim.module.mqtt.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.vivans.dcim.shared.persistence.BaseEntity;

/**
 * MQTT broker/topic 단위 공통 수집 소스.
 * device_mqtt_identity는 메시지 안 장비를 식별하는 매핑일 뿐 MQTT 연결 단위가 아니다.
 */
@Entity
@Table(name = "mqtt_source", uniqueConstraints = @UniqueConstraint(name = "uk_mqtt_source_name", columnNames = "name"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MqttSource extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private MqttSourceType sourceType;

    @Column(name = "broker_url", nullable = false, length = 500)
    private String brokerUrl;

    @Column(nullable = false, length = 500)
    private String topic;

    @Column(name = "client_id", length = 255)
    private String clientId;

    @Column(name = "credential_key", length = 100)
    private String credentialKey;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "config_version", nullable = false)
    private long configVersion;

    private MqttSource(String name, MqttSourceType sourceType, String brokerUrl, String topic,
                           String clientId, String credentialKey, boolean enabled) {
        apply(name, sourceType, brokerUrl, topic, clientId, credentialKey, enabled);
        this.configVersion = 1;
    }

    public static MqttSource create(String name, MqttSourceType sourceType, String brokerUrl, String topic,
                                        String clientId, String credentialKey, boolean enabled) {
        return new MqttSource(name, sourceType, brokerUrl, topic, clientId, credentialKey, enabled);
    }

    public void update(String name, MqttSourceType sourceType, String brokerUrl, String topic,
                       String clientId, String credentialKey, boolean enabled) {
        apply(name, sourceType, brokerUrl, topic, clientId, credentialKey, enabled);
        this.configVersion++;
    }

    private void apply(String name, MqttSourceType sourceType, String brokerUrl, String topic,
                       String clientId, String credentialKey, boolean enabled) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (sourceType == null) throw new IllegalArgumentException("sourceType is required");
        if (brokerUrl == null || brokerUrl.isBlank()) throw new IllegalArgumentException("brokerUrl is required");
        if (topic == null || topic.isBlank()) throw new IllegalArgumentException("topic is required");
        if (name.trim().length() > 100 || brokerUrl.trim().length() > 500 || topic.trim().length() > 500
                || (clientId != null && clientId.trim().length() > 255)
                || (credentialKey != null && credentialKey.trim().length() > 100)) {
            throw new IllegalArgumentException("MQTT source exceeds column limits");
        }
        validateConnection(brokerUrl.trim(), topic.trim());
        this.name = name.trim();
        this.sourceType = sourceType;
        this.brokerUrl = brokerUrl.trim();
        this.topic = topic.trim();
        this.clientId = trimToNull(clientId);
        this.credentialKey = trimToNull(credentialKey);
        this.enabled = enabled;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static void validateConnection(String broker, String filter) {
        java.net.URI uri;
        try { uri = java.net.URI.create(broker); }
        catch (IllegalArgumentException exception) { throw new IllegalArgumentException("Invalid MQTT broker URL"); }
        if (!java.util.Set.of("tcp", "ssl", "ws", "wss").contains(uri.getScheme() == null ? "" : uri.getScheme())
                || uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null
                || uri.getPort() == 0 || uri.getPort() > 65535) {
            throw new IllegalArgumentException("MQTT broker URL requires tcp/ssl/ws/wss host without embedded credentials");
        }
        if (filter.indexOf('\u0000') >= 0) throw new IllegalArgumentException("Invalid MQTT topic filter");
        String[] levels = filter.split("/", -1);
        for (int i = 0; i < levels.length; i++) {
            String level = levels[i];
            if ((level.contains("+") && !level.equals("+"))
                    || (level.contains("#") && (!level.equals("#") || i != levels.length - 1))) {
                throw new IllegalArgumentException("MQTT wildcards must occupy an entire level; # must be last");
            }
        }
    }
}
