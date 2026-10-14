package net.vivans.dcim.module.mqtt.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.mqtt.domain.model.DeviceModelMqttPoint;
import net.vivans.dcim.module.mqtt.domain.repository.DeviceModelMqttPointRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DeviceModelMqttPointJpaRepository implements DeviceModelMqttPointRepository {

    private final DeviceModelMqttPointSpringDataRepository springDataRepository;

    @Override
    public DeviceModelMqttPoint save(DeviceModelMqttPoint point) {
        return springDataRepository.save(point);
    }

    @Override
    public Optional<DeviceModelMqttPoint> findById(Integer id) {
        return springDataRepository.findById(id);
    }

    @Override
    public List<DeviceModelMqttPoint> findAllByDeviceModelIdOrderByIdAsc(Integer deviceModelId) {
        return springDataRepository.findAllByDeviceModelIdOrderByIdAsc(deviceModelId);
    }

    @Override
    public List<DeviceModelMqttPoint> findAllEnabledByDeviceModelIdIn(Collection<Integer> deviceModelIds) {
        if (deviceModelIds == null || deviceModelIds.isEmpty()) {
            return List.of();
        }
        return springDataRepository.findAllEnabledByDeviceModelIdIn(deviceModelIds);
    }

    @Override
    public List<DeviceModelMqttPoint> findAllEnabled() {
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
    public void delete(DeviceModelMqttPoint point) {
        springDataRepository.delete(point);
    }
}
