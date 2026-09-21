package net.vivans.dcim.module.lora.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.lora.domain.model.DeviceLoraEndpoint;
import net.vivans.dcim.module.lora.domain.model.LoraIdType;
import net.vivans.dcim.module.lora.domain.repository.DeviceLoraEndpointRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DeviceLoraEndpointJpaRepository implements DeviceLoraEndpointRepository {

    private final DeviceLoraEndpointSpringDataRepository springDataRepository;

    @Override
    public DeviceLoraEndpoint save(DeviceLoraEndpoint endpoint) {
        return springDataRepository.save(endpoint);
    }

    @Override
    public Optional<DeviceLoraEndpoint> findById(Integer id) {
        return springDataRepository.findById(id);
    }

    @Override
    public List<DeviceLoraEndpoint> findAllByDeviceId(Integer deviceId) {
        return springDataRepository.findAllByDeviceId(deviceId);
    }

    @Override
    public List<DeviceLoraEndpoint> findAllOrderByIdAsc() {
        return springDataRepository.findAllByOrderByIdAsc();
    }

    @Override
    public Optional<DeviceLoraEndpoint> findByIdTypeAndNormalizedExternalIdAndEnabledTrue(
            LoraIdType idType, String normalizedExternalId) {
        return springDataRepository.findByIdTypeAndNormalizedExternalIdAndEnabledTrue(idType, normalizedExternalId);
    }

    @Override
    public boolean existsByDeviceIdAndIdType(Integer deviceId, LoraIdType idType) {
        return springDataRepository.existsByDeviceIdAndIdType(deviceId, idType);
    }

    @Override
    public boolean existsByDeviceIdAndIdTypeAndIdNot(Integer deviceId, LoraIdType idType, Integer id) {
        return springDataRepository.existsByDeviceIdAndIdTypeAndIdNot(deviceId, idType, id);
    }

    @Override
    public void delete(DeviceLoraEndpoint endpoint) {
        springDataRepository.delete(endpoint);
    }
}
