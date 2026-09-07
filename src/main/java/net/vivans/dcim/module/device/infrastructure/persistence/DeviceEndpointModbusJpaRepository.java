package net.vivans.dcim.module.device.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.domain.model.DeviceEndpointModbus;
import net.vivans.dcim.module.device.domain.repository.DeviceEndpointModbusRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DeviceEndpointModbusJpaRepository implements DeviceEndpointModbusRepository {
    private final DeviceEndpointModbusSpringDataRepository springDataRepository;

    @Override
    public DeviceEndpointModbus save(DeviceEndpointModbus endpointModbus) {
        return springDataRepository.save(endpointModbus);
    }

    @Override
    public Optional<DeviceEndpointModbus> findByEndpointId(Integer endpointId) {
        return springDataRepository.findByEndpointId(endpointId);
    }
    @Override
    public boolean existsByEndpointId(Integer endpointId) {
        return springDataRepository.existsByEndpointId(endpointId);
    }


    @Override
    public void delete(DeviceEndpointModbus endpointModbus) {
        springDataRepository.delete(endpointModbus);
    }
}
