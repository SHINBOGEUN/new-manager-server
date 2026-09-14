package net.vivans.dcim.module.device.api.dto;

import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.model.DeviceAsset;
import net.vivans.dcim.module.devicegroup.api.dto.DeviceGroupSummaryResponse;

import java.util.Comparator;
import java.util.List;
import java.time.LocalDate;

public record DeviceResponse(
        Integer id,
        Integer modelId,
        String modelName,
        String manufacturer,
        String deviceTypeCode,
        String locationNodeCode,
        String locationNodeName,
        Integer pathCodeId,
        String pathCode,
        String pathName,
        String name,
        String description,
        boolean enabled,
        String assetCode,
        String serialNumber,
        Integer assetStatusId,
        String assetStatusCode,
        String assetStatusName,
        String assetColor,
        LocalDate installedDate,
        String assetManagerName,
        String supplierName,
        LocalDate warrantyExpiresOn,
        List<DeviceGroupSummaryResponse> deviceGroups
) {

    public static DeviceResponse from(Device device) {
        return from(device, device.getDeviceGroups().stream()
                .sorted(Comparator.comparing(group -> group.getName()))
                .map(DeviceGroupSummaryResponse::from)
                .toList());
    }

    public static DeviceResponse from(Device device, List<DeviceGroupSummaryResponse> deviceGroups) {
        DeviceAsset asset = device.getAsset();
        Integer pathId = null;
        String pathCode = null;
        String pathName = null;
        if (device.getPathCode() != null) {
            pathId = device.getPathCode().getId();
            pathCode = device.getPathCode().getCode();
            pathName = device.getPathCode().getName();
        }
        return new DeviceResponse(
                device.getId(),
                device.getDeviceModel().getId(),
                device.getDeviceModel().getName(),
                device.getDeviceModel().getManufacturer(),
                device.getDeviceModel().getDeviceType().getCode(),
                device.getLocationNode().getCode(),
                device.getLocationNode().getName(),
                pathId,
                pathCode,
                pathName,
                device.getName(),
                device.getDescription(),
                device.isEnabled(),
                asset == null ? null : asset.getAssetCode(),
                asset == null ? null : asset.getSerialNumber(),
                asset == null || asset.getAssetStatus() == null ? null : asset.getAssetStatus().getId(),
                asset == null || asset.getAssetStatus() == null ? null : asset.getAssetStatus().getCode(),
                asset == null || asset.getAssetStatus() == null ? null : asset.getAssetStatus().getName(),
                asset == null ? null : asset.getAssetColor(),
                asset == null ? null : asset.getInstalledDate(),
                asset == null ? null : asset.getAssetManagerName(),
                asset == null ? null : asset.getSupplierName(),
                asset == null ? null : asset.getWarrantyExpiresOn(),
                deviceGroups
        );
    }
}
