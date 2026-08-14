package net.vivans.dcim.module.device.domain.repository;

import net.vivans.dcim.module.device.domain.model.DevicePage;

import java.util.List;
import java.util.Optional;

public interface DevicePageRepository {

    DevicePage save(DevicePage devicePage);

    List<DevicePage> saveAll(Iterable<DevicePage> devicePages);

    Optional<DevicePage> findByIdAndDeviceId(Integer id, Integer deviceId);

    List<DevicePage> findAllByDeviceIdOrderByIdAsc(Integer deviceId);

    boolean existsByDeviceIdAndPageCodeId(Integer deviceId, Integer pageCodeId);

    void delete(DevicePage devicePage);

    void deleteAll(Iterable<DevicePage> devicePages);
}
