package net.vivans.dcim.module.device.infrastructure.persistence;

import java.util.List;
import net.vivans.dcim.module.device.domain.model.DeviceRackPlacementHistory;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeviceRackPlacementHistorySpringDataRepository extends JpaRepository<DeviceRackPlacementHistory, Integer> {
    @EntityGraph(attributePaths = "device")
    List<DeviceRackPlacementHistory> findAllByDevice_IdOrderByCreatedDtDesc(Integer deviceId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM DeviceRackPlacementHistory history WHERE history.device.id = :deviceId")
    void deleteByDeviceId(@Param("deviceId") Integer deviceId);
}
