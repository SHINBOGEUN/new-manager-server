package net.vivans.dcim.module.device.domain.repository;

import net.vivans.dcim.module.device.domain.model.DeviceRackPlacement;

import java.util.List;
import java.util.Optional;

public interface DeviceRackPlacementRepository {
    DeviceRackPlacement save(DeviceRackPlacement placement);
    Optional<DeviceRackPlacement> findByDeviceId(Integer deviceId);
    List<DeviceRackPlacement> findAllByRackLocationCode(String rackLocationCode);
    boolean existsOverlapping(String rackLocationCode, int startU, int endU, Integer excludeDeviceId);
    void delete(DeviceRackPlacement placement);
}
