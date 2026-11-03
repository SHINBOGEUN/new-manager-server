package net.vivans.dcim.module.device.api.dto;

import java.util.List;

public record DevicePageDisplayResponse(
        String pageCode,
        String pageName,
        String viewMode,
        String layoutAssetKey,
        List<SceneItem> items
) {
    public record SceneItem(
            Integer id,
            Integer parentId,
            String itemType,
            Integer deviceId,
            String locationNodeCode,
            String pointName,
            String label,
            String modelKey,
            double x,
            double y,
            double z,
            double rotationY,
            double sizeX,
            double sizeY,
            double sizeZ,
            String groupKey,
            String groupName,
            boolean locked
    ) {}
}
