package net.vivans.dcim.module.device.domain.repository;

import java.util.List;
import net.vivans.dcim.module.device.domain.model.DeviceRackPlacementHistory;

public interface DeviceRackPlacementHistoryRepository {
    DeviceRackPlacementHistory save(DeviceRackPlacementHistory history);
    List<DeviceRackPlacementHistory> findAllByDeviceId(Integer deviceId);
    void deleteByDeviceId(Integer deviceId);
}
