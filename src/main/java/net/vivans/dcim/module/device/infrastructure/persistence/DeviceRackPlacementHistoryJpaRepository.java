package net.vivans.dcim.module.device.infrastructure.persistence;

import java.util.List;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.domain.model.DeviceRackPlacementHistory;
import net.vivans.dcim.module.device.domain.repository.DeviceRackPlacementHistoryRepository;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DeviceRackPlacementHistoryJpaRepository implements DeviceRackPlacementHistoryRepository {
    private final DeviceRackPlacementHistorySpringDataRepository springDataRepository;

    @Override
    public DeviceRackPlacementHistory save(DeviceRackPlacementHistory history) {
        return springDataRepository.save(history);
    }

    @Override
    public List<DeviceRackPlacementHistory> findAllByDeviceId(Integer deviceId) {
        return springDataRepository.findAllByDevice_IdOrderByCreatedDtDesc(deviceId);
    }
}
