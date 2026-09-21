package net.vivans.dcim.module.lora.domain.repository;

import net.vivans.dcim.module.lora.domain.model.DeviceLoraEndpoint;
import net.vivans.dcim.module.lora.domain.model.LoraIdType;

import java.util.List;
import java.util.Optional;

public interface DeviceLoraEndpointRepository {

    DeviceLoraEndpoint save(DeviceLoraEndpoint endpoint);

    Optional<DeviceLoraEndpoint> findById(Integer id);

    List<DeviceLoraEndpoint> findAllByDeviceId(Integer deviceId);

    List<DeviceLoraEndpoint> findAllOrderByIdAsc();

    Optional<DeviceLoraEndpoint> findByIdTypeAndNormalizedExternalIdAndEnabledTrue(
            LoraIdType idType, String normalizedExternalId);

    boolean existsByDeviceIdAndIdType(Integer deviceId, LoraIdType idType);

    boolean existsByDeviceIdAndIdTypeAndIdNot(Integer deviceId, LoraIdType idType, Integer id);

    void delete(DeviceLoraEndpoint endpoint);
}
