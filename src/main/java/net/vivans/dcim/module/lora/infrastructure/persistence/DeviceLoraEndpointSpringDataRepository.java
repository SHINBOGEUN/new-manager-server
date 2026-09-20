package net.vivans.dcim.module.lora.infrastructure.persistence;

import net.vivans.dcim.module.lora.domain.model.DeviceLoraEndpoint;
import net.vivans.dcim.module.lora.domain.model.LoraIdType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceLoraEndpointSpringDataRepository extends JpaRepository<DeviceLoraEndpoint, Integer> {

    @EntityGraph(attributePaths = {"device", "device.deviceModel"})
    List<DeviceLoraEndpoint> findAllByDeviceId(Integer deviceId);

    @EntityGraph(attributePaths = {"device", "device.deviceModel"})
    List<DeviceLoraEndpoint> findAllByOrderByIdAsc();

    @EntityGraph(attributePaths = {"device", "device.deviceModel"})
    Optional<DeviceLoraEndpoint> findByIdTypeAndNormalizedExternalIdAndEnabledTrue(
            LoraIdType idType, String normalizedExternalId);

    boolean existsByDeviceIdAndIdType(Integer deviceId, LoraIdType idType);

    boolean existsByDeviceIdAndIdTypeAndIdNot(Integer deviceId, LoraIdType idType, Integer id);
}
