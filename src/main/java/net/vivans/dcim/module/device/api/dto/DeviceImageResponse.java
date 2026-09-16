package net.vivans.dcim.module.device.api.dto;

import net.vivans.dcim.module.device.domain.model.DeviceImage;

public record DeviceImageResponse(
        Integer id, Integer deviceId, String originalName, String contentType,
        long fileSize, int sortOrder, boolean primary, String contentUrl
) {
    public static DeviceImageResponse from(DeviceImage image) {
        return new DeviceImageResponse(image.getId(), image.getDevice().getId(), image.getOriginalName(),
                image.getContentType(), image.getFileSize(), image.getSortOrder(), image.isPrimary(),
                "/api/manager/assets/" + image.getDevice().getId() + "/images/" + image.getId() + "/content");
    }
}
