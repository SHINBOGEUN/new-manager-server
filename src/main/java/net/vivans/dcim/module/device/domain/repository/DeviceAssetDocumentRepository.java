package net.vivans.dcim.module.device.domain.repository;

import net.vivans.dcim.module.device.domain.model.DeviceAssetDocument;

import java.util.List;
import java.util.Optional;

public interface DeviceAssetDocumentRepository {
    DeviceAssetDocument save(DeviceAssetDocument document);
    List<DeviceAssetDocument> findAllByDeviceId(Integer deviceId);
    long countByDeviceId(Integer deviceId);
    Optional<DeviceAssetDocument> findByIdAndDeviceId(Integer documentId, Integer deviceId);
    void delete(DeviceAssetDocument document);
}
