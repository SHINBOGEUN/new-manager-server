package net.vivans.dcim.module.devicegroup.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.devicegroup.domain.model.DeviceGroup;
import net.vivans.dcim.module.devicegroup.domain.repository.DeviceGroupRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DeviceGroupJpaRepository implements DeviceGroupRepository {

    private final DeviceGroupSpringDataRepository springDataRepository;

    @Override
    public List<DeviceGroup> findAll(Boolean enabled) {
        return enabled == null
                ? springDataRepository.findAllByOrderByNameAsc()
                : springDataRepository.findByEnabledOrderByNameAsc(enabled);
    }

    @Override
    public List<DeviceGroup> findAllByDeviceIds(Collection<Integer> deviceIds) {
        if (deviceIds == null || deviceIds.isEmpty()) {
            return List.of();
        }
        return springDataRepository.findAllByDeviceIds(deviceIds);
    }

    @Override
    public Optional<DeviceGroup> findById(Integer id) {
        return springDataRepository.findById(id);
    }

    @Override
    public boolean existsByName(String name) {
        return springDataRepository.existsByName(name);
    }

    @Override
    public boolean existsByNameAndIdNot(String name, Integer id) {
        return springDataRepository.existsByNameAndIdNot(name, id);
    }

    @Override
    public DeviceGroup save(DeviceGroup deviceGroup) {
        return springDataRepository.save(deviceGroup);
    }

    @Override
    public void delete(DeviceGroup deviceGroup) {
        springDataRepository.delete(deviceGroup);
    }
}
