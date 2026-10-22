package net.vivans.dcim.module.device.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.domain.model.DeviceMonitoringPolicy;
import net.vivans.dcim.module.device.domain.repository.DeviceMonitoringPolicyRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DeviceMonitoringPolicyJpaRepository implements DeviceMonitoringPolicyRepository {
    private final DeviceMonitoringPolicySpringDataRepository repository;

    @Override public Optional<DeviceMonitoringPolicy> findByDeviceId(Integer deviceId) { return repository.findByDeviceId(deviceId); }
    @Override public List<DeviceMonitoringPolicy> findAll() { return repository.findAll(); }
    @Override public List<DeviceMonitoringPolicy> findAllByDeviceIdIn(Collection<Integer> deviceIds) {
        return deviceIds == null || deviceIds.isEmpty() ? List.of() : repository.findAllByDeviceIdIn(deviceIds);
    }
    @Override public DeviceMonitoringPolicy save(DeviceMonitoringPolicy policy) { return repository.save(policy); }
}
