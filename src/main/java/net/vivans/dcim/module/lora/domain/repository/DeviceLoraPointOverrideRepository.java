package net.vivans.dcim.module.lora.domain.repository;

import net.vivans.dcim.module.lora.domain.model.DeviceLoraPointOverride;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DeviceLoraPointOverrideRepository {

    DeviceLoraPointOverride save(DeviceLoraPointOverride override);

    Optional<DeviceLoraPointOverride> findById(Integer id);

    List<DeviceLoraPointOverride> findAllByDeviceIdOrderByIdAsc(Integer deviceId);

    List<DeviceLoraPointOverride> findAllEnabledByDeviceIdIn(Collection<Integer> deviceIds);

    List<DeviceLoraPointOverride> findAllEnabled();

    boolean existsByDeviceIdAndPayloadField(Integer deviceId, String payloadField);

    boolean existsByDeviceIdAndPayloadFieldAndIdNot(Integer deviceId, String payloadField, Integer id);

    boolean existsByDeviceIdAndPointName(Integer deviceId, String pointName);

    boolean existsByDeviceIdAndPointNameAndIdNot(Integer deviceId, String pointName, Integer id);

    void delete(DeviceLoraPointOverride override);
}
