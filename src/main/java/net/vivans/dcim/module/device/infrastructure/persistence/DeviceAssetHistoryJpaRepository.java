package net.vivans.dcim.module.device.infrastructure.persistence;

import java.util.List;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.domain.model.DeviceAssetHistory;
import net.vivans.dcim.module.device.domain.repository.DeviceAssetHistoryRepository;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DeviceAssetHistoryJpaRepository implements DeviceAssetHistoryRepository {
    private final DeviceAssetHistorySpringDataRepository springDataRepository;

    @Override
    public DeviceAssetHistory save(DeviceAssetHistory history) {
        return springDataRepository.save(history);
    }

    @Override
    public List<DeviceAssetHistory> findAllByDeviceId(Integer deviceId) {
        return springDataRepository.findAllByDevice_IdOrderByCreatedDtDesc(deviceId);
    }

    @Override
    public void deleteByDeviceId(Integer deviceId) {
        springDataRepository.deleteByDeviceId(deviceId);
    }
}
