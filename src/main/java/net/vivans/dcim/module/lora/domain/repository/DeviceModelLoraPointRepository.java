package net.vivans.dcim.module.lora.domain.repository;

import net.vivans.dcim.module.lora.domain.model.DeviceModelLoraPoint;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DeviceModelLoraPointRepository {

    DeviceModelLoraPoint save(DeviceModelLoraPoint point);

    Optional<DeviceModelLoraPoint> findById(Integer id);

    List<DeviceModelLoraPoint> findAllByDeviceModelIdOrderByIdAsc(Integer deviceModelId);

    List<DeviceModelLoraPoint> findAllEnabledByDeviceModelIdIn(Collection<Integer> deviceModelIds);

    List<DeviceModelLoraPoint> findAllEnabled();

    boolean existsByDeviceModelIdAndPayloadField(Integer deviceModelId, String payloadField);

    boolean existsByDeviceModelIdAndPayloadFieldAndIdNot(Integer deviceModelId, String payloadField, Integer id);

    boolean existsByDeviceModelIdAndPointName(Integer deviceModelId, String pointName);

    boolean existsByDeviceModelIdAndPointNameAndIdNot(Integer deviceModelId, String pointName, Integer id);

    void delete(DeviceModelLoraPoint point);
}
