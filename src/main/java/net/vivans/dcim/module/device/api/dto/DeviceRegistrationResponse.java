package net.vivans.dcim.module.device.api.dto;

public record DeviceRegistrationResponse(
        DeviceResponse device,
        Integer endpointId,
        Integer taskId,
        Integer groupId,
        java.util.List<net.vivans.dcim.module.collectortask.api.dto.DeviceCollectionSettingResponse> collection
) {
    public DeviceRegistrationResponse(DeviceResponse device,Integer endpointId,Integer taskId,Integer groupId){this(device,endpointId,taskId,groupId,java.util.List.of());}
}
