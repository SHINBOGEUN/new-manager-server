package net.vivans.dcim.module.devicemodel.domain.model;

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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.shared.persistence.BaseEntity;

@Entity
@Table(name = "device_model_modbus_point")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeviceModelModbusPoint extends BaseEntity {

    private static final String MODBUS_PROTOCOL_CODE = "modbus";
    private static final int ADDRESS_MIN = 0;
    private static final int ADDRESS_MAX = 65535;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "model_protocol_id", nullable = false)
    private DeviceModelProtocol modelProtocol;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
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

    @Enumerated(EnumType.STRING)
    @Column(name = "register_type", nullable = false, length = 30)
    private ModbusRegisterType registerType;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false, length = 20)
    private ModbusDataType dataType;

    @Enumerated(EnumType.STRING)
    @Column(name = "byte_order", length = 10)
    private ModbusByteOrder byteOrder;

    // 고정 주소면 값 보유, 인스턴스가 주소를 주면 null (requiresInstance=true)
    private Integer address;

    // true = 주소를 인스턴스(endpoint_modbus)가 제공 (분전반 회로 등)
    @Column(name = "requires_instance", nullable = false)
    private boolean requiresInstance;

    private Double scale;

    @Column(name = "value_offset")
    private Double offset;

    @Column(nullable = false)
    private boolean enabled;


    private DeviceModelModbusPoint(
            DeviceModelProtocol modelProtocol,
            String name,
            CommonCode dataPointType,
            CommonCode categoryCode,
            ModbusRegisterType registerType,
            ModbusDataType dataType,
            ModbusByteOrder byteOrder,
            Integer address,
            boolean requiresInstance,
            Double scale,
            Double offset,
            CommonCode unitCode,
            boolean enabled
    ) {
        validateModelProtocol(modelProtocol);
        validateName(name);
        validateDataPointType(dataPointType);
        validateCategoryCode(categoryCode);
        validateRegisterType(registerType);
        validateDataType(dataType);
        validateByteOrder(dataType, byteOrder);
        validateAddress(requiresInstance, address);
        validateOffset(registerType, offset);
        this.modelProtocol = modelProtocol;
        this.name = name;
        this.dataPointType = dataPointType;
        this.categoryCode = categoryCode;
        this.registerType = registerType;
        this.dataType = dataType;
        this.byteOrder = byteOrder;
        this.address = address;
        this.requiresInstance = requiresInstance;
        this.scale = scale;
        this.offset = offset;
        this.unitCode = unitCode;
        this.enabled = enabled;
    }

    public static DeviceModelModbusPoint create(
            DeviceModelProtocol modelProtocol,
            String name,
            CommonCode dataPointType,
            CommonCode categoryCode,
            ModbusRegisterType registerType,
            ModbusDataType dataType,
            ModbusByteOrder byteOrder,
            Integer address,
            boolean requiresInstance,
            Double scale,
            Double offset,
            CommonCode unitCode,
            boolean enabled
    ) {
        return new DeviceModelModbusPoint(
                modelProtocol, name, dataPointType, categoryCode, registerType, dataType, byteOrder,
                address, requiresInstance, scale, offset, unitCode, enabled
        );
    }

    public static DeviceModelModbusPoint create(
            DeviceModelProtocol modelProtocol, String name, CommonCode dataPointType,
            ModbusRegisterType registerType, ModbusDataType dataType, ModbusByteOrder byteOrder,
            Integer address, boolean requiresInstance, Double scale, Double offset,
            CommonCode unitCode, boolean enabled
    ) {
        return create(modelProtocol, name, dataPointType, null, registerType, dataType, byteOrder,
                address, requiresInstance, scale, offset, unitCode, enabled);
    }

    public static DeviceModelModbusPoint create(
            DeviceModelProtocol modelProtocol, String name, CommonCode dataPointType,
            CommonCode categoryCode,
            ModbusRegisterType registerType,
            ModbusDataType dataType, ModbusByteOrder byteOrder, Integer address,
            boolean requiresInstance, Double scale, CommonCode unitCode, boolean enabled
    ) {
        return create(modelProtocol, name, dataPointType, categoryCode, registerType, dataType, byteOrder,
                address, requiresInstance, scale, null, unitCode, enabled);
    }

    public static DeviceModelModbusPoint create(
            DeviceModelProtocol modelProtocol, String name, CommonCode dataPointType,
            ModbusRegisterType registerType, ModbusDataType dataType, ModbusByteOrder byteOrder,
            Integer address, boolean requiresInstance, Double scale, CommonCode unitCode, boolean enabled
    ) {
        return create(modelProtocol, name, dataPointType, null, registerType, dataType, byteOrder,
                address, requiresInstance, scale, null, unitCode, enabled);
    }

    public void update(
            String name,
            CommonCode dataPointType,
            CommonCode categoryCode,
            ModbusRegisterType registerType,
            ModbusDataType dataType,
            ModbusByteOrder byteOrder,
            Integer address,
            boolean requiresInstance,
            Double scale,
            Double offset,
            CommonCode unitCode,
            boolean enabled
    ) {
        validateName(name);
        validateDataPointType(dataPointType);
        validateCategoryCode(categoryCode);
        validateRegisterType(registerType);
        validateDataType(dataType);
        validateByteOrder(dataType, byteOrder);
        validateAddress(requiresInstance, address);
        validateOffset(registerType, offset);
        this.name = name;
        this.dataPointType = dataPointType;
        this.categoryCode = categoryCode;
        this.registerType = registerType;
        this.dataType = dataType;
        this.byteOrder = byteOrder;
        this.address = address;
        this.requiresInstance = requiresInstance;
        this.scale = scale;
        this.offset = offset;
        this.unitCode = unitCode;
        this.enabled = enabled;
    }

    public void update(
            String name, CommonCode dataPointType, ModbusRegisterType registerType,
            ModbusDataType dataType, ModbusByteOrder byteOrder, Integer address,
            boolean requiresInstance, Double scale, Double offset, CommonCode unitCode, boolean enabled
    ) {
        update(name, dataPointType, null, registerType, dataType, byteOrder, address,
                requiresInstance, scale, offset, unitCode, enabled);
    }

    public void update(
            String name, CommonCode dataPointType, CommonCode categoryCode, ModbusRegisterType registerType,
            ModbusDataType dataType, ModbusByteOrder byteOrder, Integer address, boolean requiresInstance,
            Double scale, CommonCode unitCode, boolean enabled
    ) {
        update(name, dataPointType, categoryCode, registerType, dataType, byteOrder, address, requiresInstance,
                scale, null, unitCode, enabled);
    }

    public void update(
            String name, CommonCode dataPointType, ModbusRegisterType registerType,
            ModbusDataType dataType, ModbusByteOrder byteOrder, Integer address,
            boolean requiresInstance, Double scale, CommonCode unitCode, boolean enabled
    ) {
        update(name, dataPointType, null, registerType, dataType, byteOrder, address,
                requiresInstance, scale, null, unitCode, enabled);
    }


    public void setPhase(CommonCode phase) {
        if (phase != null && !"PHASE".equals(phase.getCodeGroup().getGroupKey())) {
            throw new IllegalArgumentException("phase must belong to PHASE group");
        }
        this.phase = phase;
    }

    public DeviceModelModbusPoint withPhase(CommonCode phase) {
        setPhase(phase);
        return this;
    }
    private static void validateModelProtocol(DeviceModelProtocol modelProtocol) {
        if (modelProtocol == null) {
            throw new IllegalArgumentException("modelProtocol is required");
        }
        if (!MODBUS_PROTOCOL_CODE.equals(modelProtocol.getProtocolType().getCode())) {
            throw new IllegalArgumentException("protocol must be modbus");
        }
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
    }

    private static void validateDataPointType(CommonCode dataPointType) {
        if (dataPointType == null) {
            throw new IllegalArgumentException("dataPointType is required");
        }
        if (!"DATA_POINT_TYPE".equals(dataPointType.getCodeGroup().getGroupKey())) {
            throw new IllegalArgumentException("dataPointType must belong to DATA_POINT_TYPE group");
        }
    }

    private static void validateCategoryCode(CommonCode categoryCode) {
        if (categoryCode != null && !"CATEGORY".equals(categoryCode.getCodeGroup().getGroupKey())) {
            throw new IllegalArgumentException("categoryCode must belong to CATEGORY group");
        }
    }

    private static void validateRegisterType(ModbusRegisterType registerType) {
        if (registerType == null) {
            throw new IllegalArgumentException("registerType is required");
        }
    }

    private static void validateDataType(ModbusDataType dataType) {
        if (dataType == null) {
            throw new IllegalArgumentException("dataType is required");
        }
    }

    private static void validateByteOrder(ModbusDataType dataType, ModbusByteOrder byteOrder) {
        if (dataType.isMultiRegister() && byteOrder == null) {
            throw new IllegalArgumentException("byteOrder is required for multi-register data type");
        }
        if (!dataType.isMultiRegister() && byteOrder != null) {
            throw new IllegalArgumentException("byteOrder must be null for single-register data type");
        }
    }

    private static void validateAddress(boolean requiresInstance, Integer address) {
        if (requiresInstance && address != null) {
            throw new IllegalArgumentException("address must be null when requiresInstance is true");
        }
        if (!requiresInstance && address == null) {
            throw new IllegalArgumentException("address is required when requiresInstance is false");
        }
        if (address != null && (address < ADDRESS_MIN || address > ADDRESS_MAX)) {
            throw new IllegalArgumentException("address must be between 0 and 65535");
        }
    }

    private static void validateOffset(ModbusRegisterType registerType, Double offset) {
        if (offset != null && !Double.isFinite(offset)) {
            throw new IllegalArgumentException("offset must be finite");
        }
        if (offset != null && offset != 0.0 &&
                (registerType == ModbusRegisterType.COIL || registerType == ModbusRegisterType.DISCRETE)) {
            throw new IllegalArgumentException("offset is not supported for bit reads");
        }
    }

    /** 공통 코드의 표시 단위. 기존 조회·수집 설정 소비자 호환을 위해 제공한다. */
    public String getUnit() {
        return unitCode == null ? null : unitCode.getName();
    }

    public String getCategory() {
        return categoryCode == null ? null : categoryCode.getName();
    }
}

