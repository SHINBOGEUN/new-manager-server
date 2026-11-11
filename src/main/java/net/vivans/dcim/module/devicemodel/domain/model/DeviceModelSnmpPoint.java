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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_code_id")
    private CommonCode categoryCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_code_id")
    private CommonCode unitCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phase_code_id")
    private CommonCode phase;

    @Column(nullable = false, length = 512)
    private String oid;

    @Column(name = "requires_instance", nullable = false)
    private boolean requiresInstance;

    /** 원시값 배율. null이면 collector가 1.0으로 취급 */
    private Double scale;

    @Column(nullable = false)
    private boolean enabled;

    private DeviceModelSnmpPoint(
            DeviceModelProtocol modelProtocol,
            String name,
            String oid,
            boolean requiresInstance,
            CommonCode unitCode,
            Double scale,
            boolean enabled,
            CommonCode categoryCode
    ) {
        validateModelProtocol(modelProtocol);
        validateName(name);
        validateOid(oid, requiresInstance);
        validateScale(scale);
        validateCategoryCode(categoryCode);
        this.modelProtocol = modelProtocol;
        this.name = name;
        this.oid = oid;
        this.requiresInstance = requiresInstance;
        this.unitCode = unitCode;
        this.scale = scale;
        this.enabled = enabled;
        this.categoryCode = categoryCode;
    }

    public static DeviceModelSnmpPoint create(
            DeviceModelProtocol modelProtocol,
            String name,
            String oid,
            boolean requiresInstance,
            CommonCode unitCode,
            Double scale,
            boolean enabled
    ) {
        return new DeviceModelSnmpPoint(modelProtocol, name, oid, requiresInstance, unitCode, scale, enabled, null);
    }

    public static DeviceModelSnmpPoint create(
            DeviceModelProtocol modelProtocol, String name, String oid, boolean requiresInstance,
            CommonCode unitCode, Double scale, boolean enabled, CommonCode dataPointType) {
        DeviceModelSnmpPoint point = create(modelProtocol, name, oid, requiresInstance, unitCode, scale, enabled);
        point.dataPointType = dataPointType;
        return point;
    }

    public static DeviceModelSnmpPoint create(
            DeviceModelProtocol modelProtocol, String name, String oid, boolean requiresInstance,
            CommonCode unitCode, Double scale, boolean enabled, CommonCode dataPointType, CommonCode categoryCode) {
        validateCategoryCode(categoryCode);
        DeviceModelSnmpPoint point = create(modelProtocol, name, oid, requiresInstance, unitCode, scale, enabled);
        point.dataPointType = dataPointType;
        point.categoryCode = categoryCode;
        return point;
    }

    public void update(
            String name,
            String oid,
            boolean requiresInstance,
            CommonCode unitCode,
            Double scale,
            boolean enabled
    ) {
        validateName(name);
        validateOid(oid, requiresInstance);
        validateScale(scale);
        this.name = name;
        this.oid = oid;
        this.requiresInstance = requiresInstance;
        this.unitCode = unitCode;
        this.scale = scale;
        this.enabled = enabled;
        this.categoryCode = null;
    }

    public void update(String name, String oid, boolean requiresInstance, CommonCode unitCode, Double scale,
                       boolean enabled, CommonCode dataPointType, CommonCode categoryCode) {
        validateCategoryCode(categoryCode);
        update(name, oid, requiresInstance, unitCode, scale, enabled);
        this.dataPointType = dataPointType;
        this.categoryCode = categoryCode;
    }

    public String getUnit() {
        return unitCode == null ? null : unitCode.getName();
    }

    public String getCategory() {
        return categoryCode == null ? null : categoryCode.getName();
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

    public void setPhase(CommonCode phase) {
        if (phase != null && !"PHASE".equals(phase.getCodeGroup().getGroupKey())) {
            throw new IllegalArgumentException("phase must belong to PHASE group");
        }
        this.phase = phase;
    }

    public DeviceModelSnmpPoint withPhase(CommonCode phase) {
        setPhase(phase);
        return this;
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
        // SNMP tools commonly display an OID with a leading dot (e.g. .1.3.6...).
        // Accept that notation while still rejecting empty segments elsewhere.
        if (normalized.startsWith(".")) {
            normalized = normalized.substring(1);
        }
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

    private static void validateCategoryCode(CommonCode categoryCode) {
        if (categoryCode != null && !"CATEGORY".equals(categoryCode.getCodeGroup().getGroupKey())) {
            throw new IllegalArgumentException("categoryCode must belong to CATEGORY group");
        }
    }
}
