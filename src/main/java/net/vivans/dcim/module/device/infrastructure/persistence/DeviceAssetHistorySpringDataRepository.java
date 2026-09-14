package net.vivans.dcim.module.device.infrastructure.persistence;

import java.util.List;
import net.vivans.dcim.module.device.domain.model.DeviceAssetHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceAssetHistorySpringDataRepository extends JpaRepository<DeviceAssetHistory, Integer> {
    List<DeviceAssetHistory> findAllByDevice_IdOrderByCreatedDtDesc(Integer deviceId);
}
