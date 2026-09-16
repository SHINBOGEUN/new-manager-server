package net.vivans.dcim.module.device.api.dto;

import net.vivans.dcim.module.device.domain.model.DeviceAssetDocument;

import java.time.Instant;

public record DeviceAssetDocumentResponse(
        Integer id, Integer deviceId, String originalName, String contentType, long fileSize,
        Instant uploadedAt, String contentUrl
) {
    public static DeviceAssetDocumentResponse from(DeviceAssetDocument document) {
        return new DeviceAssetDocumentResponse(document.getId(), document.getDevice().getId(), document.getOriginalName(),
                document.getContentType(), document.getFileSize(), document.getCreatedDt(),
                "/api/manager/assets/" + document.getDevice().getId() + "/documents/" + document.getId() + "/content");
    }
}
