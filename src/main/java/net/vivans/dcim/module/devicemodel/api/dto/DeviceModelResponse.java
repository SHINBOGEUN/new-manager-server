package net.vivans.dcim.module.devicemodel.api.dto;

import net.vivans.dcim.module.devicemodel.domain.model.DeviceModel;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModelProtocol;

import java.util.ArrayList;
import java.util.List;

public record DeviceModelResponse(
        Integer id,
        String name,
        String manufacturer,
        Integer deviceTypeId,
        String deviceTypeCode,
        String deviceTypeName,
        String description,
        List<DeviceModelProtocolResponse> protocols,
        String phaseNotation,
        java.util.Map<String, String> phaseLabels
) {

    public static DeviceModelResponse from(DeviceModel deviceModel) {
        List<DeviceModelProtocolResponse> protocolResponses = new ArrayList<>();
        for (DeviceModelProtocol protocol : deviceModel.getSortedProtocols()) {
            protocolResponses.add(DeviceModelProtocolResponse.from(protocol));
        }
        return new DeviceModelResponse(
                deviceModel.getId(),
                deviceModel.getName(),
                deviceModel.getManufacturer(),
                deviceModel.getDeviceType().getId(),
                deviceModel.getDeviceType().getCode(),
                deviceModel.getDeviceType().getName(),
                deviceModel.getDescription(),
                protocolResponses,
                deviceModel.getPhaseNotation() == null ? null : deviceModel.getPhaseNotation().getCode(),
                deviceModel.phaseLabels()
        );
    }
}
