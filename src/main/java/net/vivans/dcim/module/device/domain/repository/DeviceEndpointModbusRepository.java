package net.vivans.dcim.module.device.domain.repository;

import net.vivans.dcim.module.device.domain.model.DeviceEndpointModbus;

import java.util.Optional;

public interface DeviceEndpointModbusRepository {

    DeviceEndpointModbus save(DeviceEndpointModbus endpointModbus);

    Optional<DeviceEndpointModbus> findByEndpointId(Integer endpointId);

    boolean existsByEndpointId(Integer endpointId);

    void delete(DeviceEndpointModbus endpointModbus);
}
