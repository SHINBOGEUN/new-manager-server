package net.vivans.dcim.module.device.domain.repository;

import net.vivans.dcim.module.device.domain.model.DeviceProtocolEndpoint;

import java.util.List;
import java.util.Optional;

public interface DeviceProtocolEndpointRepository {

    DeviceProtocolEndpoint save(DeviceProtocolEndpoint endpoint);

    Optional<DeviceProtocolEndpoint> findById(Integer id);

    Optional<DeviceProtocolEndpoint> findByIdAndDeviceId(Integer id, Integer deviceId);

    List<DeviceProtocolEndpoint> findAllByDeviceIdOrderByIdAsc(Integer deviceId);

    boolean existsByDeviceIdAndProtocolTypeId(Integer deviceId, Integer protocolTypeId);

    boolean existsByDeviceIdAndProtocolTypeIdAndIdNot(Integer deviceId, Integer protocolTypeId, Integer id);

    boolean existsByHostAndPort(String host, int port);

    boolean existsByHostAndPortAndIdNot(String host, int port, Integer id);

    void delete(DeviceProtocolEndpoint endpoint);
}
