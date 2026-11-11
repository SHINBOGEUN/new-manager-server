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
            boolean locked,
            String rackColor,
            String panelPhoto,
            boolean panelLayoutInitialized,
            /** 카탈로그의 모델 역할(RACK/PANEL/FRAME) 또는 null. 화면은 SR_* 키 이름 대신 이 값으로 판단한다. */
            String modelRole
    ) {}
}
