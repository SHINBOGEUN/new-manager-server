package net.vivans.dcim.module.devicemodel.domain.repository;

import net.vivans.dcim.module.devicemodel.domain.model.DeviceModelModbusPoint;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DeviceModelModbusPointRepository {

    DeviceModelModbusPoint save(DeviceModelModbusPoint modelModbusPoint);

    Optional<DeviceModelModbusPoint> findByIdAndModelProtocolId(Integer id, Integer modelProtocolId);

    List<DeviceModelModbusPoint> findAllByModelProtocolIdOrderByIdAsc(Integer modelProtocolId);

    List<DeviceModelModbusPoint> findAllEnabledByDeviceModelIds(Collection<Integer> deviceModelIds);

    boolean existsByModelProtocolIdAndName(Integer modelProtocolId, String name);

    boolean existsByModelProtocolIdAndNameAndIdNot(Integer modelProtocolId, String name, Integer id);

    void delete(DeviceModelModbusPoint modelModbusPoint);
}
