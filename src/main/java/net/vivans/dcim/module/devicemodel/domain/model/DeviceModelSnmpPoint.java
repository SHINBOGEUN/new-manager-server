package net.vivans.dcim.module.devicemodel.domain.model;

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
import net.vivans.dcim.shared.persistence.BaseEntity;
import net.vivans.dcim.module.common.domain.model.CommonCode;

@Entity
@Table(
        name = "device_model_snmp_point",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_device_model_snmp_point_protocol_name",
                        columnNames = {"model_protocol_id", "name"}
                ),
                @UniqueConstraint(
                        name = "uk_device_model_snmp_point_protocol_oid",
                        columnNames = {"model_protocol_id", "oid"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeviceModelSnmpPoint extends BaseEntity {

    static final String INSTANCE_ID_PLACEHOLDER = "{instanceId}";
    private static final String SNMP_PROTOCOL_CODE = "snmp";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "model_protocol_id", nullable = false)
    private DeviceModelProtocol modelProtocol;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "data_point_type_id", nullable = false)
    private CommonCode dataPointType;

    @Column(nullable = false, length = 512)
    private String oid;

    @Column(name = "requires_instance", nullable = false)
    private boolean requiresInstance;

    @Column(length = 50)
    private String unit;

    /** 원시값 배율. null이면 collector가 1.0으로 취급 */
    private Double scale;

    @Column(nullable = false)
    private boolean enabled;

    private DeviceModelSnmpPoint(
            DeviceModelProtocol modelProtocol,
            String name,
            String oid,
            boolean requiresInstance,
            String unit,
            Double scale,
            boolean enabled
    ) {
        validateModelProtocol(modelProtocol);
        validateName(name);
        validateOid(oid, requiresInstance);
        validateScale(scale);
        this.modelProtocol = modelProtocol;
        this.name = name;
        this.oid = oid;
        this.requiresInstance = requiresInstance;
        this.unit = unit;
        this.scale = scale;
        this.enabled = enabled;
    }

    public static DeviceModelSnmpPoint create(
            DeviceModelProtocol modelProtocol,
            String name,
            String oid,
            boolean requiresInstance,
            String unit,
            Double scale,
            boolean enabled
    ) {
        return new DeviceModelSnmpPoint(modelProtocol, name, oid, requiresInstance, unit, scale, enabled);
    }

    public static DeviceModelSnmpPoint create(
            DeviceModelProtocol modelProtocol, String name, String oid, boolean requiresInstance,
            String unit, Double scale, boolean enabled, CommonCode dataPointType) {
        DeviceModelSnmpPoint point = new DeviceModelSnmpPoint(modelProtocol, name, oid, requiresInstance, unit, scale, enabled);
        point.dataPointType = dataPointType;
        return point;
    }

    public void update(
            String name,
            String oid,
            boolean requiresInstance,
            String unit,
            Double scale,
            boolean enabled
    ) {
        validateName(name);
        validateOid(oid, requiresInstance);
        validateScale(scale);
        this.name = name;
        this.oid = oid;
        this.requiresInstance = requiresInstance;
        this.unit = unit;
        this.scale = scale;
        this.enabled = enabled;
    }

    public void update(String name, String oid, boolean requiresInstance, String unit, Double scale,
                       boolean enabled, CommonCode dataPointType) {
        update(name, oid, requiresInstance, unit, scale, enabled);
        this.dataPointType = dataPointType;
    }

    public String resolveOid(Integer instanceId) {
        if (!requiresInstance) {
            return oid;
        }
        if (instanceId == null) {
            return null;
        }
        return oid.replace(INSTANCE_ID_PLACEHOLDER, String.valueOf(instanceId));
    }

    private static void validateModelProtocol(DeviceModelProtocol modelProtocol) {
        if (modelProtocol == null) {
            throw new IllegalArgumentException("modelProtocol is required");
        }
        if (!SNMP_PROTOCOL_CODE.equals(modelProtocol.getProtocolType().getCode())) {
            throw new IllegalArgumentException("protocol must be snmp");
        }
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
    }

    private static void validateOid(String oid, boolean requiresInstance) {
        if (oid == null || oid.isBlank()) {
            throw new IllegalArgumentException("oid is required");
        }
        boolean hasPlaceholder = oid.contains(INSTANCE_ID_PLACEHOLDER);
        if (requiresInstance && !hasPlaceholder) {
            throw new IllegalArgumentException("oid must contain {instanceId}");
        }
        if (!requiresInstance && hasPlaceholder) {
            throw new IllegalArgumentException("oid must not contain {instanceId}");
        }
        validateOidFormat(oid);
    }

    private static void validateOidFormat(String oid) {
        String normalized = oid.replace(INSTANCE_ID_PLACEHOLDER, "0");
        if (!normalized.matches("^[0-9.]+$")) {
            throw new IllegalArgumentException("invalid oid format");
        }
        for (String segment : normalized.split("\\.")) {
            if (segment.isEmpty()) {
                throw new IllegalArgumentException("invalid oid format");
            }
        }
    }

    private static void validateScale(Double scale) {
        if (scale == null) {
            return;
        }
        if (!Double.isFinite(scale)) {
            throw new IllegalArgumentException("scale must be a finite number");
        }
    }
}
