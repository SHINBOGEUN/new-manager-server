package net.vivans.dcim.module.device.infrastructure.persistence;

import net.vivans.dcim.module.device.domain.model.DevicePage;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DevicePageSpringDataRepository extends JpaRepository<DevicePage, Integer> {

    @EntityGraph(attributePaths = {"device", "pageCode", "pageCode.codeGroup"})
    Optional<DevicePage> findByIdAndDevice_Id(Integer id, Integer deviceId);

    @EntityGraph(attributePaths = {"device", "pageCode", "pageCode.codeGroup"})
    List<DevicePage> findAllByDevice_IdOrderByIdAsc(Integer deviceId);

    boolean existsByDevice_IdAndPageCode_Id(Integer deviceId, Integer pageCodeId);
}
