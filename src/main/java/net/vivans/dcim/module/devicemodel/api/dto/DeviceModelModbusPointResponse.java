package net.vivans.dcim.module.devicemodel.api.dto;

import net.vivans.dcim.module.devicemodel.domain.model.DeviceModelModbusPoint;
import net.vivans.dcim.module.devicemodel.domain.model.ModbusByteOrder;
import net.vivans.dcim.module.devicemodel.domain.model.ModbusDataType;
import net.vivans.dcim.module.devicemodel.domain.model.ModbusRegisterType;
public record DeviceModelModbusPointResponse(
        Integer id,
        Integer modelId,
        Integer protocolId,
        String name,
        Integer dataPointTypeId,
        String dataPointType,
        Integer categoryCodeId,
        String categoryCode,
        String categoryName,
        Integer unitCodeId,
        String unit,
        ModbusRegisterType registerType,
        ModbusDataType dataType,
        ModbusByteOrder byteOrder,
        Integer address,
        boolean requiresInstance,
        Double scale,
        Double offset,
        boolean enabled,
        String phase
) {

    public static DeviceModelModbusPointResponse from(DeviceModelModbusPoint point) {
        return new DeviceModelModbusPointResponse(
                point.getId(),
                point.getModelProtocol().getId(),
                point.getModelProtocol().getId(),
                point.getName(),
                point.getDataPointType().getId(),
                point.getDataPointType().getCode(),
                point.getCategoryCode() == null ? null : point.getCategoryCode().getId(),
                point.getCategoryCode() == null ? null : point.getCategoryCode().getCode(),
                point.getCategoryCode() == null ? null : point.getCategoryCode().getName(),
                point.getUnitCode() == null ? null : point.getUnitCode().getId(),
                point.getUnit(),
                point.getRegisterType(),
                point.getDataType(),
                point.getByteOrder(),
                point.getAddress(),
                point.isRequiresInstance(),
                point.getScale(),
                point.getOffset(),
                point.isEnabled(),
                point.getPhase() == null ? null : point.getPhase().getCode()
        );
    }
}
