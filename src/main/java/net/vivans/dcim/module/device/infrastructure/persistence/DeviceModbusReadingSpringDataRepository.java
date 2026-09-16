package net.vivans.dcim.module.device.infrastructure.persistence;

import net.vivans.dcim.module.device.domain.model.DeviceModbusReading;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceModbusReadingSpringDataRepository extends JpaRepository<DeviceModbusReading, Integer> {

    @EntityGraph(attributePaths = {"endpointModbus", "endpointModbus.endpoint", "endpointModbus.endpoint.device", "point", "targetDevice"})
    List<DeviceModbusReading> findAllByEndpointModbus_EndpointIdOrderByIdAsc(Integer endpointId);

    @EntityGraph(attributePaths = {"endpointModbus", "endpointModbus.endpoint", "endpointModbus.endpoint.device", "point", "targetDevice"})
    Optional<DeviceModbusReading> findByIdAndEndpointModbus_EndpointId(Integer id, Integer endpointId);

    boolean existsByEndpointModbus_EndpointIdAndUnitIdAndAddress(Integer endpointId, int unitId, int address);

    boolean existsByEndpointModbus_EndpointIdAndUnitIdAndAddressAndIdNot(Integer endpointId, int unitId, int address, Integer id);

    boolean existsByTargetDevice_IdAndPointName(Integer targetDeviceId, String pointName);

    boolean existsByTargetDevice_IdAndPointNameAndIdNot(Integer targetDeviceId, String pointName, Integer id);

    boolean existsByTargetDevice_Id(Integer targetDeviceId);
}
