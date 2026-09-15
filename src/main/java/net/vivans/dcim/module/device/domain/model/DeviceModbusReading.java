package net.vivans.dcim.module.device.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModelModbusPoint;
import net.vivans.dcim.shared.persistence.BaseEntity;

@Entity
@Table(name = "device_modbus_reading")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeviceModbusReading extends BaseEntity {

    private static final String MODBUS_PROTOCOL_CODE = "modbus";

    private static final int UNIT_ID_MIN = 0;
    private static final int UNIT_ID_MAX = 247;

    private static final int ADDRESS_MIN = 0;
    private static final int ADDRESS_MAX = 65535;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "endpoint_id", nullable = false)
    private DeviceProtocolEndpoint endpoint;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "point_id", nullable = false)
    private DeviceModelModbusPoint point;

    @Column(name = "unit_id", nullable = false)
    private int unitId;

    @Column(nullable = false)
    private int address;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_device_id", nullable = false)
    private Device targetDevice;

    // 결과를 저장할 필드명. 모델 point의 이름과 다를 수 있음
    @Column(name = "point_name", nullable = false)
    private String pointName;

    @Column(nullable = false)
    private boolean enabled;

    private DeviceModbusReading(
            DeviceProtocolEndpoint endpoint,
            DeviceModelModbusPoint point,
            int unitId,
            int address,
            Device targetDevice,
            String pointName,
            boolean enabled
    ) {
        validateEndpoint(endpoint);
        validatePoint(point);
        validateUnitId(unitId);
        validateAddress(address, point);
        validateTargetDevice(targetDevice);
        validatePointName(pointName);

        this.endpoint = endpoint;
        this.point = point;
        this.unitId = unitId;
        this.address = address;
        this.targetDevice = targetDevice;
        this.pointName = pointName;
        this.enabled = enabled;
    }

    public static DeviceModbusReading create(
            DeviceProtocolEndpoint endpoint,
            DeviceModelModbusPoint point,
            int unitId,
            int address,
            Device targetDevice,
            String pointName,
            boolean enabled
    ) {
        return new DeviceModbusReading(
                endpoint,
                point,
                unitId,
                address,
                targetDevice,
                pointName,
                enabled
        );
    }

    public void update(
            DeviceModelModbusPoint point,
            int unitId,
            int address,
            Device targetDevice,
            String pointName,
            boolean enabled
    ) {
        validateEndpoint(this.endpoint);
        validatePoint(point);
        validateUnitId(unitId);
        validateAddress(address, point);
        validateTargetDevice(targetDevice);
        validatePointName(pointName);

        this.point = point;
        this.unitId = unitId;
        this.address = address;
        this.targetDevice = targetDevice;
        this.pointName = pointName;
        this.enabled = enabled;
    }

    private static void validateEndpoint(DeviceProtocolEndpoint endpoint) {
        if (endpoint == null) {
            throw new IllegalArgumentException("endpoint is required");
        }

        if (!MODBUS_PROTOCOL_CODE.equals(
                endpoint.getProtocolType().getCode()
        )) {
            throw new IllegalArgumentException(
                    "endpoint protocol must be modbus"
            );
        }
    }

    private static void validatePoint(DeviceModelModbusPoint point) {
        if (point == null) {
            throw new IllegalArgumentException("point is required");
        }

        if (!point.isRequiresInstance()) {
            throw new IllegalArgumentException(
                    "point must require instance mapping"
            );
        }
    }

    private static void validateUnitId(int unitId) {
        if (unitId < UNIT_ID_MIN || unitId > UNIT_ID_MAX) {
            throw new IllegalArgumentException(
                    "unitId must be between 0 and 247"
            );
        }
    }

    private static void validateAddress(
            int address,
            DeviceModelModbusPoint point
    ) {
        if (address < ADDRESS_MIN || address > ADDRESS_MAX) {
            throw new IllegalArgumentException(
                    "address must be between 0 and 65535"
            );
        }

        int registerCount = point.getDataType().getRegisterCount();
        long lastAddress = (long) address + registerCount - 1;

        if (lastAddress > ADDRESS_MAX) {
            throw new IllegalArgumentException(
                    "address range exceeds 65535"
            );
        }
    }

    private static void validateTargetDevice(Device targetDevice) {
        if (targetDevice == null) {
            throw new IllegalArgumentException(
                    "targetDevice is required"
            );
        }
    }

    private static void validatePointName(String pointName) {
        if (pointName == null || pointName.isBlank()) {
            throw new IllegalArgumentException(
                    "pointName is required"
            );
        }
    }
}