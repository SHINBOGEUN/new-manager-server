package net.vivans.dcim.module.device.api.dto;

import net.vivans.dcim.module.device.domain.model.CctvCamera;

public record CctvCameraResponse(Integer id, String cameraKey, String name, String streamUrl, int sortOrder) {
    public static CctvCameraResponse from(CctvCamera camera) {
        return new CctvCameraResponse(camera.getId(), camera.getCameraKey(), camera.getName(),
                camera.getStreamUrl(), camera.getSortOrder());
    }
}
