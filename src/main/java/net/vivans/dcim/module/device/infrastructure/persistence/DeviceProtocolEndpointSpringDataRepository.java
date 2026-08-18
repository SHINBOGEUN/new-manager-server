package net.vivans.dcim.module.device.infrastructure.persistence;

import net.vivans.dcim.module.device.domain.model.DeviceProtocolEndpoint;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceProtocolEndpointSpringDataRepository extends JpaRepository<DeviceProtocolEndpoint, Integer> {

    @EntityGraph(attributePaths = {"device", "protocolType", "protocolType.codeGroup"})
    Optional<DeviceProtocolEndpoint> findById(Integer id);

    @EntityGraph(attributePaths = {"device", "protocolType", "protocolType.codeGroup"})
    Optional<DeviceProtocolEndpoint> findByIdAndDevice_Id(Integer id, Integer deviceId);

    @EntityGraph(attributePaths = {"device", "protocolType", "protocolType.codeGroup"})
    List<DeviceProtocolEndpoint> findAllByDevice_IdOrderByIdAsc(Integer deviceId);

    boolean existsByDevice_IdAndProtocolType_Id(Integer deviceId, Integer protocolTypeId);

    boolean existsByDevice_IdAndProtocolType_IdAndIdNot(Integer deviceId, Integer protocolTypeId, Integer id);

    boolean existsByHostAndPort(String host, int port);

    boolean existsByHostAndPortAndIdNot(String host, int port, Integer id);
}
