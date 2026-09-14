package net.vivans.dcim.module.devicegroup.infrastructure.persistence;

import net.vivans.dcim.module.devicegroup.domain.model.DeviceGroup;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DeviceGroupSpringDataRepository extends JpaRepository<DeviceGroup, Integer> {

    @EntityGraph(attributePaths = {"devices", "devices.deviceModel", "devices.locationNode"})
    List<DeviceGroup> findAllByOrderByNameAsc();

    @EntityGraph(attributePaths = {"devices", "devices.deviceModel", "devices.locationNode"})
    List<DeviceGroup> findByEnabledOrderByNameAsc(boolean enabled);

    @Query("SELECT DISTINCT g FROM DeviceGroup g JOIN FETCH g.devices d WHERE d.id IN :deviceIds ORDER BY g.name ASC")
    List<DeviceGroup> findAllByDeviceIds(@Param("deviceIds") Collection<Integer> deviceIds);

    @EntityGraph(attributePaths = {"devices", "devices.deviceModel", "devices.locationNode"})
    Optional<DeviceGroup> findById(Integer id);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Integer id);
}
