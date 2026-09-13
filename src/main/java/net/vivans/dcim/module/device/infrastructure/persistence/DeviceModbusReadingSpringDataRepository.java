package net.vivans.dcim.module.device.infrastructure.persistence;

import net.vivans.dcim.module.device.domain.model.DeviceModbusReading;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceModbusReadingSpringDataRepository extends JpaRepository<DeviceModbusReading, Integer> {

    @EntityGraph(attributePaths = {"endpoint", "point", "targetDevice"})
    List<DeviceModbusReading> findAllByEndpoint_IdOrderByIdAsc(Integer endpointId);

    @EntityGraph(attributePaths = {"endpoint", "point", "targetDevice"})
    Optional<DeviceModbusReading> findByIdAndEndpoint_Id(Integer id, Integer endpointId);

    boolean existsByEndpoint_IdAndUnitIdAndAddress(Integer endpointId, int unitId, int address);

    boolean existsByEndpoint_IdAndUnitIdAndAddressAndIdNot(Integer endpointId, int unitId, int address, Integer id);

    boolean existsByTargetDevice_IdAndPointName(Integer targetDeviceId, String pointName);

    boolean existsByTargetDevice_IdAndPointNameAndIdNot(Integer targetDeviceId, String pointName, Integer id);

    boolean existsByTargetDevice_Id(Integer targetDeviceId);
}