package net.vivans.dcim.module.lora.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.shared.persistence.BaseEntity;

/**
 * 장비 단위 LoRa payload 필드 매핑 예외(override).
 * 매핑 우선순위: DeviceLoraPointOverride → DeviceModelLoraPoint → 없으면 미매핑 오류.
 */
@Entity
@Table(
        name = "device_lora_point_override",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_device_lora_point_override_device_field",
                        columnNames = {"device_id", "payload_field"}
                ),
                @UniqueConstraint(
                        name = "uk_device_lora_point_override_device_name",
                        columnNames = {"device_id", "point_name"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeviceLoraPointOverride extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(name = "payload_field", nullable = false)
    private String payloadField;

    @Column(name = "point_name", nullable = false)
    private String pointName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "data_point_type_id", nullable = false)
    private CommonCode dataPointType;

    @Column(length = 50)
    private String unit;

    private Double scale;

    @Column(name = "value_map", length = 1000)
    private String valueMap;

    @Column(nullable = false)
    private boolean enabled;

    private DeviceLoraPointOverride(
            Device device, String payloadField, String pointName, CommonCode dataPointType,
            String unit, Double scale, String valueMap, boolean enabled
    ) {
        validateDevice(device);
        validatePayloadField(payloadField);
        validatePointName(pointName);
        validateDataPointType(dataPointType);
        this.device = device;
        this.payloadField = payloadField.trim();
        this.pointName = pointName.trim();
        this.dataPointType = dataPointType;
        this.unit = unit;
        this.scale = scale;
        this.valueMap = valueMap;
        this.enabled = enabled;
    }

    public static DeviceLoraPointOverride create(
            Device device, String payloadField, String pointName, CommonCode dataPointType,
            String unit, Double scale, String valueMap, boolean enabled
    ) {
        return new DeviceLoraPointOverride(device, payloadField, pointName, dataPointType, unit, scale, valueMap, enabled);
    }

    public void update(
            String payloadField, String pointName, CommonCode dataPointType,
            String unit, Double scale, String valueMap, boolean enabled
    ) {
        validatePayloadField(payloadField);
        validatePointName(pointName);
        validateDataPointType(dataPointType);
        this.payloadField = payloadField.trim();
        this.pointName = pointName.trim();
        this.dataPointType = dataPointType;
        this.unit = unit;
        this.scale = scale;
        this.valueMap = valueMap;
        this.enabled = enabled;
    }

    private static void validateDevice(Device device) {
        if (device == null) {
            throw new IllegalArgumentException("device is required");
        }
    }

    private static void validatePayloadField(String payloadField) {
        if (payloadField == null || payloadField.isBlank()) {
            throw new IllegalArgumentException("payloadField is required");
        }
    }

    private static void validatePointName(String pointName) {
        if (pointName == null || pointName.isBlank()) {
            throw new IllegalArgumentException("pointName is required");
        }
    }

    private static void validateDataPointType(CommonCode dataPointType) {
        if (dataPointType == null) {
            throw new IllegalArgumentException("dataPointType is required");
        }
    }
}
