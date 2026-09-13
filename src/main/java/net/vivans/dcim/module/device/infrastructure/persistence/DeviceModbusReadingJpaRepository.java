package net.vivans.dcim.module.device.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.domain.model.DeviceModbusReading;
import net.vivans.dcim.module.device.domain.repository.DeviceModbusReadingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DeviceModbusReadingJpaRepository
        implements DeviceModbusReadingRepository {

    private final DeviceModbusReadingSpringDataRepository springDataRepository;

    @Override
    public DeviceModbusReading save(DeviceModbusReading reading) {
        return springDataRepository.save(reading);
    }

    @Override
    public List<DeviceModbusReading> findAllByEndpointIdOrderByIdAsc(
            Integer endpointId
    ) {
        return springDataRepository.findAllByEndpoint_IdOrderByIdAsc(endpointId);
    }

    @Override
    public Optional<DeviceModbusReading> findByIdAndEndpointId(
            Integer id,
            Integer endpointId
    ) {
        return springDataRepository.findByIdAndEndpoint_Id(id, endpointId);
    }

    @Override
    public boolean existsByEndpointIdAndUnitIdAndAddress(
            Integer endpointId,
            int unitId,
            int address
    ) {
        return springDataRepository.existsByEndpoint_IdAndUnitIdAndAddress(
                endpointId, unitId, address
        );
    }

    @Override
    public boolean existsByEndpointIdAndUnitIdAndAddressAndIdNot(
            Integer endpointId,
            int unitId,
            int address,
            Integer id
    ) {
        return springDataRepository.existsByEndpoint_IdAndUnitIdAndAddressAndIdNot(
                endpointId, unitId, address, id
        );
    }

    @Override
    public boolean existsByTargetDeviceIdAndPointName(
            Integer targetDeviceId,
            String pointName
    ) {
        return springDataRepository.existsByTargetDevice_IdAndPointName(
                targetDeviceId, pointName
        );
    }

    @Override
    public boolean existsByTargetDeviceIdAndPointNameAndIdNot(
            Integer targetDeviceId,
            String pointName,
            Integer id
    ) {
        return springDataRepository.existsByTargetDevice_IdAndPointNameAndIdNot(
                targetDeviceId, pointName, id
        );
    }

    @Override
    public boolean existsByTargetDeviceId(Integer targetDeviceId) {
        return springDataRepository.existsByTargetDevice_Id(targetDeviceId);
    }

    @Override
    public void delete(DeviceModbusReading reading) {
        springDataRepository.delete(reading);
    }
}