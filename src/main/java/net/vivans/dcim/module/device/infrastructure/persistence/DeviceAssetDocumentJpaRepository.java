package net.vivans.dcim.module.device.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.domain.model.DeviceAssetDocument;
import net.vivans.dcim.module.device.domain.repository.DeviceAssetDocumentRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DeviceAssetDocumentJpaRepository implements DeviceAssetDocumentRepository {
    private final DeviceAssetDocumentSpringDataRepository springDataRepository;

    @Override public DeviceAssetDocument save(DeviceAssetDocument document) { return springDataRepository.save(document); }
    @Override public List<DeviceAssetDocument> findAllByDeviceId(Integer deviceId) { return springDataRepository.findAllByDevice_IdOrderByCreatedDtDescIdDesc(deviceId); }
    @Override public long countByDeviceId(Integer deviceId) { return springDataRepository.countByDevice_Id(deviceId); }
    @Override public Optional<DeviceAssetDocument> findByIdAndDeviceId(Integer documentId, Integer deviceId) { return springDataRepository.findByIdAndDevice_Id(documentId, deviceId); }
    @Override public void delete(DeviceAssetDocument document) { springDataRepository.delete(document); }
}
