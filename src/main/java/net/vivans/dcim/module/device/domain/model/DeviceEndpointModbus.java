package net.vivans.dcim.module.device.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.vivans.dcim.shared.persistence.BaseEntity;

@Entity
@Table(name = "device_endpoint_modbus")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeviceEndpointModbus extends BaseEntity {

    private static final String MODBUS_PROTOCOL_CODE = "modbus";
    private static final int MIN_UNIT_ID = 0;
    private static final int MAX_UNIT_ID = 247;

    @Id
    @Column(name = "endpoint_id")
    private Integer endpointId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "endpoint_id")
    private DeviceProtocolEndpoint endpoint;

    @Column(name = "unit_id")
    private Integer unitId;

    private DeviceEndpointModbus(DeviceProtocolEndpoint endpoint, Integer unitId) {
        validateEndpoint(endpoint);
        validateUnitId(unitId);
        this.endpoint = endpoint;
        this.unitId = unitId;
    }

    public static DeviceEndpointModbus create(DeviceProtocolEndpoint endpoint, Integer unitId) {
        return new DeviceEndpointModbus(endpoint, unitId);
    }

    public void update(Integer unitId) {
        validateUnitId(unitId);
        this.unitId = unitId;
    }

    private static void validateEndpoint(DeviceProtocolEndpoint endpoint) {
        if (endpoint == null) {
            throw new IllegalArgumentException("endpoint is required");
        }
        if (!MODBUS_PROTOCOL_CODE.equals(endpoint.getProtocolType().getCode())) {
            throw new IllegalArgumentException("endpoint protocol must be modbus");
        }
    }

    private static void validateUnitId(Integer unitId) {
        if (unitId != null && (unitId < MIN_UNIT_ID || unitId > MAX_UNIT_ID)) {
            throw new IllegalArgumentException("unitId must be between 0 and 247");
        }
    }
}