package net.vivans.dcim.module.device.domain.repository;

import net.vivans.dcim.module.device.domain.model.DeviceImage;

import java.util.List;
import java.util.Optional;

public interface DeviceImageRepository {
    DeviceImage save(DeviceImage image);
    List<DeviceImage> saveAll(Iterable<DeviceImage> images);
    List<DeviceImage> findAllByDeviceId(Integer deviceId);
    Optional<DeviceImage> findByIdAndDeviceId(Integer imageId, Integer deviceId);
    void delete(DeviceImage image);
}
