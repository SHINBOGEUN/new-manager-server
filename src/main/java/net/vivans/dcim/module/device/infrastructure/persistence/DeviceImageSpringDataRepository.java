package net.vivans.dcim.module.device.infrastructure.persistence;

import net.vivans.dcim.module.device.domain.model.DeviceImage;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceImageSpringDataRepository extends JpaRepository<DeviceImage, Integer> {
    @EntityGraph(attributePaths = "device")
    List<DeviceImage> findAllByDevice_IdOrderByPrimaryDescSortOrderAscIdAsc(Integer deviceId);
    @EntityGraph(attributePaths = "device")
    Optional<DeviceImage> findByIdAndDevice_Id(Integer imageId, Integer deviceId);
}
