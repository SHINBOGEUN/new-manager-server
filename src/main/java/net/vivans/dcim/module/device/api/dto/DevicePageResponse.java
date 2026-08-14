package net.vivans.dcim.module.device.api.dto;

import net.vivans.dcim.module.device.domain.model.DevicePage;

public record DevicePageResponse(
        Integer id,
        Integer deviceId,
        Integer pageCodeId,
        String pageCode,
        String pageName
) {

    public static DevicePageResponse from(DevicePage devicePage) {
        return new DevicePageResponse(
                devicePage.getId(),
                devicePage.getDevice().getId(),
                devicePage.getPageCode().getId(),
                devicePage.getPageCode().getCode(),
                devicePage.getPageCode().getName()
        );
    }
}
