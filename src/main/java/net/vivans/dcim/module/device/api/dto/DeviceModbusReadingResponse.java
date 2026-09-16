package net.vivans.dcim.module.device.api.dto;

import net.vivans.dcim.module.device.domain.model.DeviceModbusReading;

public record DeviceModbusReadingResponse(
        Integer id,
        Integer endpointId,
        Integer sourceDeviceId,
        Integer pointId,
        int unitId,
        int address,
        Integer targetDeviceId,
        String pointName,
        boolean enabled
) {

    public static DeviceModbusReadingResponse from(
            DeviceModbusReading reading) {
        return new DeviceModbusReadingResponse(
                reading.getId(),
                reading.getEndpointModbus().getEndpointId(),
                reading.getEndpointModbus().getEndpoint().getDevice().getId(),
                reading.getPoint().getId(),
                reading.getUnitId(),
                reading.getAddress(),
                reading.getTargetDevice().getId(),
                reading.getPointName(),
                reading.isEnabled()
        );
    }
}
