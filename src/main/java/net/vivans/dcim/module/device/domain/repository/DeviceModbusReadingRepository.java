package net.vivans.dcim.module.device.domain.repository;

import net.vivans.dcim.module.device.domain.model.DeviceModbusReading;

import java.util.List;
import java.util.Optional;

public interface DeviceModbusReadingRepository {

    DeviceModbusReading save(DeviceModbusReading reading);

    List<DeviceModbusReading> findAllByEndpointIdOrderByIdAsc(Integer endpointId);

    Optional<DeviceModbusReading> findByIdAndEndpointId(Integer id, Integer endpointId);

    boolean existsByEndpointIdAndUnitIdAndAddress(Integer endpointId, int unitId, int address);

    boolean existsByEndpointIdAndUnitIdAndAddressAndIdNot(Integer endpointId, int unitId, int address, Integer id);

    boolean existsByTargetDeviceIdAndPointName(Integer targetDeviceId, String pointName);

    boolean existsByTargetDeviceIdAndPointNameAndIdNot(Integer targetDeviceId, String pointName, Integer id);

    boolean existsByTargetDeviceId(Integer targetDeviceId);

    void delete(DeviceModbusReading reading);
}