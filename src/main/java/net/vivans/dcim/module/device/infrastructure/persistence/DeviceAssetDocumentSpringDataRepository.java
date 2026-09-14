package net.vivans.dcim.module.device.infrastructure.persistence;

import net.vivans.dcim.module.device.domain.model.DeviceAssetDocument;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceAssetDocumentSpringDataRepository extends JpaRepository<DeviceAssetDocument, Integer> {
    @EntityGraph(attributePaths = "device")
    List<DeviceAssetDocument> findAllByDevice_IdOrderByCreatedDtDescIdDesc(Integer deviceId);

    long countByDevice_Id(Integer deviceId);

    @EntityGraph(attributePaths = "device")
    Optional<DeviceAssetDocument> findByIdAndDevice_Id(Integer documentId, Integer deviceId);
}
