package net.vivans.dcim.module.lora.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.lora.domain.model.DeviceLoraPointOverride;
import net.vivans.dcim.module.lora.domain.repository.DeviceLoraPointOverrideRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DeviceLoraPointOverrideJpaRepository implements DeviceLoraPointOverrideRepository {

    private final DeviceLoraPointOverrideSpringDataRepository springDataRepository;

    @Override
    public DeviceLoraPointOverride save(DeviceLoraPointOverride override) {
        return springDataRepository.save(override);
    }

    @Override
    public Optional<DeviceLoraPointOverride> findById(Integer id) {
        return springDataRepository.findById(id);
    }

    @Override
    public List<DeviceLoraPointOverride> findAllByDeviceIdOrderByIdAsc(Integer deviceId) {
        return springDataRepository.findAllByDeviceIdOrderByIdAsc(deviceId);
    }

    @Override
    public List<DeviceLoraPointOverride> findAllEnabledByDeviceIdIn(Collection<Integer> deviceIds) {
        if (deviceIds == null || deviceIds.isEmpty()) {
            return List.of();
        }
        return springDataRepository.findAllEnabledByDeviceIdIn(deviceIds);
    }

    @Override
    public List<DeviceLoraPointOverride> findAllEnabled() {
        return springDataRepository.findAllByEnabledTrue();
    }

    @Override
    public boolean existsByDeviceIdAndPayloadField(Integer deviceId, String payloadField) {
        return springDataRepository.existsByDeviceIdAndPayloadField(deviceId, payloadField);
    }

    @Override
    public boolean existsByDeviceIdAndPayloadFieldAndIdNot(Integer deviceId, String payloadField, Integer id) {
        return springDataRepository.existsByDeviceIdAndPayloadFieldAndIdNot(deviceId, payloadField, id);
    }

    @Override
    public boolean existsByDeviceIdAndPointName(Integer deviceId, String pointName) {
        return springDataRepository.existsByDeviceIdAndPointName(deviceId, pointName);
    }

    @Override
    public boolean existsByDeviceIdAndPointNameAndIdNot(Integer deviceId, String pointName, Integer id) {
        return springDataRepository.existsByDeviceIdAndPointNameAndIdNot(deviceId, pointName, id);
    }

    @Override
    public void delete(DeviceLoraPointOverride override) {
        springDataRepository.delete(override);
    }
}
