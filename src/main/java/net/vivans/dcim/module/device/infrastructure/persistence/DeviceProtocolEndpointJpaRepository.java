package net.vivans.dcim.module.device.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.domain.model.DeviceProtocolEndpoint;
import net.vivans.dcim.module.device.domain.repository.DeviceProtocolEndpointRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DeviceProtocolEndpointJpaRepository implements DeviceProtocolEndpointRepository {

    private final DeviceProtocolEndpointSpringDataRepository springDataRepository;

    @Override
    public DeviceProtocolEndpoint save(DeviceProtocolEndpoint endpoint) {
        return springDataRepository.save(endpoint);
    }

    @Override
    public Optional<DeviceProtocolEndpoint> findById(Integer id) {
        return springDataRepository.findById(id);
    }

    @Override
    public Optional<DeviceProtocolEndpoint> findByIdAndDeviceId(Integer id, Integer deviceId) {
        return springDataRepository.findByIdAndDevice_Id(id, deviceId);
    }

    @Override
    public List<DeviceProtocolEndpoint> findAllByDeviceIdOrderByIdAsc(Integer deviceId) {
        return springDataRepository.findAllByDevice_IdOrderByIdAsc(deviceId);
    }

    @Override
    public boolean existsByDeviceIdAndProtocolTypeId(Integer deviceId, Integer protocolTypeId) {
        return springDataRepository.existsByDevice_IdAndProtocolType_Id(deviceId, protocolTypeId);
    }

    @Override
    public boolean existsByDeviceIdAndProtocolTypeIdAndIdNot(
            Integer deviceId,
            Integer protocolTypeId,
            Integer id
    ) {
        return springDataRepository.existsByDevice_IdAndProtocolType_IdAndIdNot(deviceId, protocolTypeId, id);
    }

    @Override
    public boolean existsByHostAndPort(String host, int port) {
        return springDataRepository.existsByHostAndPort(host, port);
    }

    @Override
    public boolean existsByHostAndPortAndIdNot(String host, int port, Integer id) {
        return springDataRepository.existsByHostAndPortAndIdNot(host, port, id);
    }

    @Override
    public void delete(DeviceProtocolEndpoint endpoint) {
        springDataRepository.delete(endpoint);
    }
}
