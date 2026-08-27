package net.vivans.dcim.module.devicemodel.domain.repository;

import net.vivans.dcim.module.devicemodel.domain.model.DeviceModelSnmpPoint;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DeviceModelSnmpPointRepository {

    DeviceModelSnmpPoint save(DeviceModelSnmpPoint modelSnmpPoint);

    Optional<DeviceModelSnmpPoint> findByIdAndModelProtocolId(Integer id, Integer modelProtocolId);

    List<DeviceModelSnmpPoint> findAllByModelProtocolIdOrderByIdAsc(Integer modelProtocolId);

    List<DeviceModelSnmpPoint> findAllEnabledByDeviceModelIds(Collection<Integer> deviceModelIds);

    boolean existsByModelProtocolIdAndName(Integer modelProtocolId, String name);

    boolean existsByModelProtocolIdAndNameAndIdNot(Integer modelProtocolId, String name, Integer id);

    boolean existsByModelProtocolIdAndOid(Integer modelProtocolId, String oid);

    boolean existsByModelProtocolIdAndOidAndIdNot(Integer modelProtocolId, String oid, Integer id);

    boolean existsByModelProtocolIdAndRequiresInstanceTrue(Integer modelProtocolId);

    void delete(DeviceModelSnmpPoint modelSnmpPoint);
}
