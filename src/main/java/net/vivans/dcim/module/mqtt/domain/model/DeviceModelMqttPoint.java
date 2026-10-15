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
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModel;
import net.vivans.dcim.shared.persistence.BaseEntity;

/**
 * 모델별 MQTT 포인트 정의. 물리 GPU 개수와 UUID별 행을 생성하지 않는다.
 */
@Entity
@Table(
        name = "device_model_mqtt_point",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_device_model_mqtt_point_model_field",
                        columnNames = {"device_model_id", "component_path", "payload_field"}
                ),
                @UniqueConstraint(
                        name = "uk_device_model_mqtt_point_model_name",
                        columnNames = {"device_model_id", "point_name"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeviceModelMqttPoint extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_model_id", nullable = false)
    private DeviceModel deviceModel;

    @Column(name = "payload_field", nullable = false)
    private String payloadField;

    @Column(name = "point_name", nullable = false)
    private String pointName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "data_point_type_id", nullable = false)
    private CommonCode dataPointType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_code_id")
    private CommonCode categoryCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_code_id")
    private CommonCode unitCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "component_scope", nullable = false, length = 20)
    private MqttPointScope componentScope = MqttPointScope.DEVICE;

    @Column(name = "component_type", length = 50)
    private String componentType;

    @Column(name = "component_path", nullable = false)
    private String componentPath = "";

    @Column(name = "component_index_field")
    private String componentIndexField;

    @Column(name = "component_uuid_field")
    private String componentUuidField;

    private Double scale;

    @Column(name = "value_map", length = 1000)
    private String valueMap;

    @Column(nullable = false)
    private boolean enabled;

    private DeviceModelMqttPoint(
            DeviceModel deviceModel, String payloadField, String pointName, CommonCode dataPointType,
            CommonCode unitCode, Double scale, String valueMap, boolean enabled, CommonCode categoryCode
    ) {
        validateDeviceModel(deviceModel);
        validatePayloadField(payloadField);
        validatePointName(pointName);
        validateDataPointType(dataPointType);
        validateCategoryCode(categoryCode);
        this.deviceModel = deviceModel;
        this.payloadField = payloadField.trim();
        this.pointName = pointName.trim();
        this.dataPointType = dataPointType;
        this.categoryCode = categoryCode;
        this.unitCode = unitCode;
        this.scale = scale;
        this.valueMap = valueMap;
        this.enabled = enabled;
    }

    public static DeviceModelMqttPoint create(
            DeviceModel deviceModel, String payloadField, String pointName, CommonCode dataPointType,
            CommonCode unitCode, Double scale, String valueMap, boolean enabled, CommonCode categoryCode
    ) {
        return new DeviceModelMqttPoint(deviceModel, payloadField, pointName, dataPointType, unitCode, scale, valueMap, enabled, categoryCode);
    }

    public static DeviceModelMqttPoint create(
            DeviceModel deviceModel, String payloadField, String pointName, CommonCode dataPointType,
            CommonCode unitCode, Double scale, String valueMap, boolean enabled
    ) {
        return create(deviceModel, payloadField, pointName, dataPointType, unitCode, scale, valueMap, enabled, null);
    }

    public void update(
            String payloadField, String pointName, CommonCode dataPointType,
            CommonCode unitCode, Double scale, String valueMap, boolean enabled, CommonCode categoryCode
    ) {
        validatePayloadField(payloadField);
        validatePointName(pointName);
        validateDataPointType(dataPointType);
        validateCategoryCode(categoryCode);
        this.payloadField = payloadField.trim();
        this.pointName = pointName.trim();
        this.dataPointType = dataPointType;
        this.categoryCode = categoryCode;
        this.unitCode = unitCode;
        this.scale = scale;
        this.valueMap = valueMap;
        this.enabled = enabled;
    }

    public void update(
            String payloadField, String pointName, CommonCode dataPointType,
            CommonCode unitCode, Double scale, String valueMap, boolean enabled
    ) {
        update(payloadField, pointName, dataPointType, unitCode, scale, valueMap, enabled, null);
    }

    public String getUnit() {
        return unitCode == null ? null : unitCode.getName();
    }

    /** 예: type=GPU, path=gpus, indexField=gpu_index, uuidField=gpu_uuid. */
    public void configureComponent(String type, String path, String indexField, String uuidField) {
        if (type == null || type.isBlank() || type.trim().length() > 50
                || path == null || path.isBlank() || path.trim().length() > 255
                || indexField == null || indexField.isBlank() || indexField.trim().length() > 255
                || (uuidField != null && uuidField.trim().length() > 255)) {
            throw new IllegalArgumentException("component type, path and index field are required within column limits");
        }
        this.componentScope = MqttPointScope.COMPONENT;
        this.componentType = type.trim();
        this.componentPath = path.trim();
        this.componentIndexField = indexField.trim();
        this.componentUuidField = uuidField == null || uuidField.isBlank() ? null : uuidField.trim();
    }

    public void configureDevice() {
        this.componentScope = MqttPointScope.DEVICE;
        this.componentType = null;
        this.componentPath = "";
        this.componentIndexField = null;
        this.componentUuidField = null;
    }

    public String getCategory() {
        return categoryCode == null ? null : categoryCode.getName();
    }

    private static void validateDeviceModel(DeviceModel deviceModel) {
        if (deviceModel == null) {
            throw new IllegalArgumentException("deviceModel is required");
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

    private static void validateCategoryCode(CommonCode categoryCode) {
        if (categoryCode != null && !"CATEGORY".equals(categoryCode.getCodeGroup().getGroupKey())) {
            throw new IllegalArgumentException("categoryCode must belong to CATEGORY group");
        }
    }
}
