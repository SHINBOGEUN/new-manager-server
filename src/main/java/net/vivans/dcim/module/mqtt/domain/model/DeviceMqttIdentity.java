package net.vivans.dcim.module.mqtt.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.shared.persistence.BaseEntity;

/**
 * MQTT 외부 식별값과 내부 장비의 현재 연결. 식별값 변경 시 device_id를 유지한다.
 */
@Entity
@Table(
        name = "device_mqtt_identity",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_device_mqtt_identity_type_normalized",
                        columnNames = {"identity_type", "normalized_external_id"}
                ),
                @UniqueConstraint(
                        name = "uk_device_mqtt_identity_device_type",
                        columnNames = {"device_id", "identity_type"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeviceMqttIdentity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Enumerated(EnumType.STRING)
    @Column(name = "identity_type", nullable = false, length = 20)
    private MqttIdentityType identityType;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Column(name = "normalized_external_id", nullable = false)
    private String normalizedExternalId;

    @Column(nullable = false)
    private boolean enabled;

    private DeviceMqttIdentity(Device device, MqttIdentityType idType, String externalId, boolean enabled) {
        validateDevice(device);
        validateIdType(idType);
        validateExternalId(externalId);
        this.device = device;
        this.identityType = idType;
        this.externalId = externalId.trim();
        this.normalizedExternalId = MqttExternalIdNormalizer.normalize(idType, externalId);
        this.enabled = enabled;
    }

    public static DeviceMqttIdentity create(Device device, MqttIdentityType idType, String externalId, boolean enabled) {
        return new DeviceMqttIdentity(device, idType, externalId, enabled);
    }

    public void update(String externalId, boolean enabled) {
        validateExternalId(externalId);
        String normalized = MqttExternalIdNormalizer.normalize(identityType, externalId);
        this.externalId = externalId.trim();
        this.normalizedExternalId = normalized;
        this.enabled = enabled;
    }

    private static void validateDevice(Device device) {
        if (device == null) {
            throw new IllegalArgumentException("device is required");
        }
    }

    private static void validateIdType(MqttIdentityType idType) {
        if (idType == null) {
            throw new IllegalArgumentException("idType is required");
        }
    }

    private static void validateExternalId(String externalId) {
        if (externalId == null || externalId.isBlank()) {
            throw new IllegalArgumentException("externalId is required");
        }
    }
}
