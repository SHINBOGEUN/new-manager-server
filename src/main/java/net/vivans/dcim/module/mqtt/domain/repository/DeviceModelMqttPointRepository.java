package net.vivans.dcim.module.mqtt.domain.repository;

import net.vivans.dcim.module.mqtt.domain.model.DeviceModelMqttPoint;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DeviceModelMqttPointRepository {

    DeviceModelMqttPoint save(DeviceModelMqttPoint point);

    Optional<DeviceModelMqttPoint> findById(Integer id);

    List<DeviceModelMqttPoint> findAllByDeviceModelIdOrderByIdAsc(Integer deviceModelId);

    List<DeviceModelMqttPoint> findAllEnabledByDeviceModelIdIn(Collection<Integer> deviceModelIds);

    List<DeviceModelMqttPoint> findAllEnabled();

    boolean existsByDeviceModelIdAndPayloadField(Integer deviceModelId, String payloadField);

    boolean existsByDeviceModelIdAndPayloadFieldAndIdNot(Integer deviceModelId, String payloadField, Integer id);

    boolean existsByDeviceModelIdAndPointName(Integer deviceModelId, String pointName);

    boolean existsByDeviceModelIdAndPointNameAndIdNot(Integer deviceModelId, String pointName, Integer id);

    void delete(DeviceModelMqttPoint point);
}
