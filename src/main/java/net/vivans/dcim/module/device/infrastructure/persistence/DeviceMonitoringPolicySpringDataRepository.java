package net.vivans.dcim.module.device.infrastructure.persistence;

import net.vivans.dcim.module.device.domain.model.DeviceMonitoringPolicy;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DeviceMonitoringPolicySpringDataRepository extends JpaRepository<DeviceMonitoringPolicy, Integer> {
    @EntityGraph(attributePaths = {"monitoringMode", "monitoringMode.codeGroup"})
    Optional<DeviceMonitoringPolicy> findByDeviceId(Integer deviceId);

    @EntityGraph(attributePaths = {"monitoringMode", "monitoringMode.codeGroup"})
    List<DeviceMonitoringPolicy> findAll();

    @EntityGraph(attributePaths = {"monitoringMode", "monitoringMode.codeGroup"})
    List<DeviceMonitoringPolicy> findAllByDeviceIdIn(Collection<Integer> deviceIds);
}
