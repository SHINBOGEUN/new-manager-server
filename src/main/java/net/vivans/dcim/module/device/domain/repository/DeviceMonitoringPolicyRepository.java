package net.vivans.dcim.module.device.domain.repository;

import net.vivans.dcim.module.device.domain.model.DeviceMonitoringPolicy;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DeviceMonitoringPolicyRepository {
    Optional<DeviceMonitoringPolicy> findByDeviceId(Integer deviceId);
    List<DeviceMonitoringPolicy> findAll();
    List<DeviceMonitoringPolicy> findAllByDeviceIdIn(Collection<Integer> deviceIds);
    DeviceMonitoringPolicy save(DeviceMonitoringPolicy policy);
}
