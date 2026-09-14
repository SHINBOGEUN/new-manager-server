package net.vivans.dcim.module.devicegroup.domain.repository;

import net.vivans.dcim.module.devicegroup.domain.model.DeviceGroup;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DeviceGroupRepository {

    List<DeviceGroup> findAll(Boolean enabled);

    List<DeviceGroup> findAllByDeviceIds(Collection<Integer> deviceIds);

    Optional<DeviceGroup> findById(Integer id);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Integer id);

    DeviceGroup save(DeviceGroup deviceGroup);

    void delete(DeviceGroup deviceGroup);
}
