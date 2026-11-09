package net.vivans.dcim.module.device.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record DevicePageDisplayUpdateRequest(
        @NotBlank String viewMode,
        String layoutAssetKey,
        @NotNull List<@Valid SceneItem> items
) {
    public record SceneItem(
            @NotBlank String clientKey,
            String parentClientKey,
            @NotBlank String itemType,
            Integer deviceId,
            String locationNodeCode,
            String pointName,
            @NotBlank String label,
            @NotBlank String modelKey,
            Double x,
            Double y,
            Double z,
            Double rotationY,
            Double sizeX,
            Double sizeY,
            Double sizeZ,
            String groupKey,
            String groupName,
            Boolean locked,
            String rackColor,
            String panelPhoto,
            Boolean panelLayoutInitialized
    ) {}
}
