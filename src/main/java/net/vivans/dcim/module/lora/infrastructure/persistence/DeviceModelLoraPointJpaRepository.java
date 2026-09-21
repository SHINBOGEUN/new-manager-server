package net.vivans.dcim.module.lora.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.lora.domain.model.DeviceModelLoraPoint;
import net.vivans.dcim.module.lora.domain.repository.DeviceModelLoraPointRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DeviceModelLoraPointJpaRepository implements DeviceModelLoraPointRepository {

    private final DeviceModelLoraPointSpringDataRepository springDataRepository;

    @Override
    public DeviceModelLoraPoint save(DeviceModelLoraPoint point) {
        return springDataRepository.save(point);
    }

    @Override
    public Optional<DeviceModelLoraPoint> findById(Integer id) {
        return springDataRepository.findById(id);
    }

    @Override
    public List<DeviceModelLoraPoint> findAllByDeviceModelIdOrderByIdAsc(Integer deviceModelId) {
        return springDataRepository.findAllByDeviceModelIdOrderByIdAsc(deviceModelId);
    }

    @Override
    public List<DeviceModelLoraPoint> findAllEnabledByDeviceModelIdIn(Collection<Integer> deviceModelIds) {
        if (deviceModelIds == null || deviceModelIds.isEmpty()) {
            return List.of();
        }
        return springDataRepository.findAllEnabledByDeviceModelIdIn(deviceModelIds);
    }

    @Override
    public List<DeviceModelLoraPoint> findAllEnabled() {
        return springDataRepository.findAllByEnabledTrue();
    }

    @Override
    public boolean existsByDeviceModelIdAndPayloadField(Integer deviceModelId, String payloadField) {
        return springDataRepository.existsByDeviceModelIdAndPayloadField(deviceModelId, payloadField);
    }

    @Override
    public boolean existsByDeviceModelIdAndPayloadFieldAndIdNot(Integer deviceModelId, String payloadField, Integer id) {
        return springDataRepository.existsByDeviceModelIdAndPayloadFieldAndIdNot(deviceModelId, payloadField, id);
    }

    @Override
    public boolean existsByDeviceModelIdAndPointName(Integer deviceModelId, String pointName) {
        return springDataRepository.existsByDeviceModelIdAndPointName(deviceModelId, pointName);
    }

    @Override
    public boolean existsByDeviceModelIdAndPointNameAndIdNot(Integer deviceModelId, String pointName, Integer id) {
        return springDataRepository.existsByDeviceModelIdAndPointNameAndIdNot(deviceModelId, pointName, id);
    }

    @Override
    public void delete(DeviceModelLoraPoint point) {
        springDataRepository.delete(point);
    }
}
