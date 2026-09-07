package net.vivans.dcim.module.device.api.dto;

import net.vivans.dcim.module.device.domain.model.DeviceEndpointModbus;

public record DeviceEndpointModbusResponse(
        Integer endpointId,
        Integer deviceId,
        Integer unitId
) {

    public static DeviceEndpointModbusResponse from(DeviceEndpointModbus endpointModbus) {
        return new DeviceEndpointModbusResponse(
                endpointModbus.getEndpointId(),
                endpointModbus.getEndpoint().getDevice().getId(),
                endpointModbus.getUnitId()
        );
    }
}
