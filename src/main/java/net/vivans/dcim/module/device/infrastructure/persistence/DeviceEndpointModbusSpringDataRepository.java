package net.vivans.dcim.module.device.infrastructure.persistence;

import net.vivans.dcim.module.device.domain.model.DeviceEndpointModbus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeviceEndpointModbusSpringDataRepository extends JpaRepository<DeviceEndpointModbus, Integer> {

    @EntityGraph(attributePaths = {
            "endpoint",
            "endpoint.device",
            "endpoint.protocolType",
            "endpoint.protocolType.codeGroup"})
    Optional<DeviceEndpointModbus> findByEndpointId(Integer endpointId);

    boolean existsByEndpointId(Integer endpointId);
}
