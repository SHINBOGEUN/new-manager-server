package net.vivans.dcim.module.lora.domain.model;

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
 * LoRaWAN MQTT broker/topic 단위 수집 소스.
 * device_lora_endpoint는 메시지 안 장비를 식별하는 매핑일 뿐 MQTT 연결 단위가 아니다.
 */
@Entity
@Table(name = "lora_mqtt_source", uniqueConstraints = @UniqueConstraint(name = "uk_lora_mqtt_source_name", columnNames = "name"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoraMqttSource extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private LoraMqttSourceType sourceType;

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

    private LoraMqttSource(String name, LoraMqttSourceType sourceType, String brokerUrl, String topic,
                           String clientId, String credentialKey, boolean enabled) {
        apply(name, sourceType, brokerUrl, topic, clientId, credentialKey, enabled);
        this.configVersion = 1;
    }

    public static LoraMqttSource create(String name, LoraMqttSourceType sourceType, String brokerUrl, String topic,
                                        String clientId, String credentialKey, boolean enabled) {
        return new LoraMqttSource(name, sourceType, brokerUrl, topic, clientId, credentialKey, enabled);
    }

    public void update(String name, LoraMqttSourceType sourceType, String brokerUrl, String topic,
                       String clientId, String credentialKey, boolean enabled) {
        apply(name, sourceType, brokerUrl, topic, clientId, credentialKey, enabled);
        this.configVersion++;
    }

    private void apply(String name, LoraMqttSourceType sourceType, String brokerUrl, String topic,
                       String clientId, String credentialKey, boolean enabled) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (sourceType == null) throw new IllegalArgumentException("sourceType is required");
        if (brokerUrl == null || brokerUrl.isBlank()) throw new IllegalArgumentException("brokerUrl is required");
        if (topic == null || topic.isBlank()) throw new IllegalArgumentException("topic is required");
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
}
