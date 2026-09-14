package net.vivans.dcim.module.device.domain.repository;

import java.util.List;
import net.vivans.dcim.module.device.domain.model.DeviceAssetHistory;

public interface DeviceAssetHistoryRepository {
    DeviceAssetHistory save(DeviceAssetHistory history);
    List<DeviceAssetHistory> findAllByDeviceId(Integer deviceId);
}
