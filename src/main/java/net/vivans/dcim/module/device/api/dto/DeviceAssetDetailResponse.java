package net.vivans.dcim.module.device.api.dto;

import java.util.List;

public record DeviceAssetDetailResponse(
        DeviceResponse device,
        List<DeviceProtocolEndpointResponse> endpoints,
        DeviceRackPlacementResponse rackPlacement,
        List<DeviceImageResponse> images,
        List<DeviceAssetDocumentResponse> documents
) {}
