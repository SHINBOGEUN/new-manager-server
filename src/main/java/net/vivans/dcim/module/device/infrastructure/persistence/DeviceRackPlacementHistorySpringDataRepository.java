package net.vivans.dcim.module.device.infrastructure.persistence;

import java.util.List;
import net.vivans.dcim.module.device.domain.model.DeviceRackPlacementHistory;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceRackPlacementHistorySpringDataRepository extends JpaRepository<DeviceRackPlacementHistory, Integer> {
    @EntityGraph(attributePaths = "device")
    List<DeviceRackPlacementHistory> findAllByDevice_IdOrderByCreatedDtDesc(Integer deviceId);
}
