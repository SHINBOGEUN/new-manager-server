package net.vivans.dcim.module.devicemodel.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModelModbusPoint;
import net.vivans.dcim.module.devicemodel.domain.repository.DeviceModelModbusPointRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DeviceModelModbusPointJpaRepository implements DeviceModelModbusPointRepository {

    private final DeviceModelModbusPointSpringDataRepository springDataRepository;

    @Override
    public DeviceModelModbusPoint save(DeviceModelModbusPoint modelModbusPoint) {
        return springDataRepository.save(modelModbusPoint);
    }

    @Override
    public Optional<DeviceModelModbusPoint> findByIdAndModelProtocolId(Integer id, Integer modelProtocolId) {
        return springDataRepository.findByIdAndModelProtocolId(id, modelProtocolId);
    }

    @Override
    public List<DeviceModelModbusPoint> findAllByModelProtocolIdOrderByIdAsc(Integer modelProtocolId) {
        return springDataRepository.findAllByModelProtocolIdOrderByIdAsc(modelProtocolId);
    }

    @Override
    public List<DeviceModelModbusPoint> findAllEnabledByDeviceModelIds(Collection<Integer> deviceModelIds) {
        if (deviceModelIds == null || deviceModelIds.isEmpty()) {
            return List.of();
        }
        return springDataRepository.findAllEnabledByDeviceModelIds(deviceModelIds);
    }

    @Override
    public boolean existsByModelProtocolIdAndName(Integer modelProtocolId, String name) {
        return springDataRepository.existsByModelProtocolIdAndName(modelProtocolId, name);
    }

    @Override
    public boolean existsByModelProtocolIdAndNameAndIdNot(Integer modelProtocolId, String name, Integer id) {
        return springDataRepository.existsByModelProtocolIdAndNameAndIdNot(modelProtocolId, name, id);
    }

    @Override
    public void delete(DeviceModelModbusPoint modelModbusPoint) {
        springDataRepository.delete(modelModbusPoint);
    }
}
