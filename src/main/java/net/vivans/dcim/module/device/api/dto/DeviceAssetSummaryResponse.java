package net.vivans.dcim.module.device.api.dto;

import java.util.List;

public record DeviceAssetSummaryResponse(
        DeviceResponse device,
        String ipAddress,
        List<String> protocolCodes,
        DeviceRackPlacementResponse rackPlacement,
        DeviceImageResponse primaryImage
) {}
