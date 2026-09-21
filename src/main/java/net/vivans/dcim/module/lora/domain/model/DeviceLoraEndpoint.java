package net.vivans.dcim.module.lora.domain.model;

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
 * LoRa/Dragino 외부 식별자(devEUI 또는 deviceName) ↔ 내부 device 매칭.
 * 매칭 우선순위(payload에 devEUI가 있으면 devEUI, 없으면 deviceName)는 조회하는 쪽(Sensor Data)에서 적용한다.
 */
@Entity
@Table(
        name = "device_lora_endpoint",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_device_lora_endpoint_type_normalized",
                        columnNames = {"id_type", "normalized_external_id"}
                ),
                @UniqueConstraint(
                        name = "uk_device_lora_endpoint_device_type",
                        columnNames = {"device_id", "id_type"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeviceLoraEndpoint extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Enumerated(EnumType.STRING)
    @Column(name = "id_type", nullable = false, length = 20)
    private LoraIdType idType;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Column(name = "normalized_external_id", nullable = false)
    private String normalizedExternalId;

    @Column(nullable = false)
    private boolean enabled;

    private DeviceLoraEndpoint(Device device, LoraIdType idType, String externalId, boolean enabled) {
        validateDevice(device);
        validateIdType(idType);
        validateExternalId(externalId);
        this.device = device;
        this.idType = idType;
        this.externalId = externalId.trim();
        this.normalizedExternalId = LoraExternalIdNormalizer.normalize(externalId);
        this.enabled = enabled;
    }

    public static DeviceLoraEndpoint create(Device device, LoraIdType idType, String externalId, boolean enabled) {
        return new DeviceLoraEndpoint(device, idType, externalId, enabled);
    }

    public void update(String externalId, boolean enabled) {
        validateExternalId(externalId);
        this.externalId = externalId.trim();
        this.normalizedExternalId = LoraExternalIdNormalizer.normalize(externalId);
        this.enabled = enabled;
    }

    private static void validateDevice(Device device) {
        if (device == null) {
            throw new IllegalArgumentException("device is required");
        }
    }

    private static void validateIdType(LoraIdType idType) {
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
