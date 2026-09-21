package net.vivans.dcim.module.lora.infrastructure.persistence;

import net.vivans.dcim.module.lora.domain.model.DeviceLoraPointOverride;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface DeviceLoraPointOverrideSpringDataRepository extends JpaRepository<DeviceLoraPointOverride, Integer> {

    @EntityGraph(attributePaths = {"device", "dataPointType"})
    List<DeviceLoraPointOverride> findAllByDeviceIdOrderByIdAsc(Integer deviceId);

    @Query("""
            SELECT o FROM DeviceLoraPointOverride o
            WHERE o.device.id IN :deviceIds AND o.enabled = true
            """)
    @EntityGraph(attributePaths = {"device", "dataPointType"})
    List<DeviceLoraPointOverride> findAllEnabledByDeviceIdIn(@Param("deviceIds") Collection<Integer> deviceIds);

    @EntityGraph(attributePaths = {"device", "dataPointType"})
    List<DeviceLoraPointOverride> findAllByEnabledTrue();

    boolean existsByDeviceIdAndPayloadField(Integer deviceId, String payloadField);

    boolean existsByDeviceIdAndPayloadFieldAndIdNot(Integer deviceId, String payloadField, Integer id);

    boolean existsByDeviceIdAndPointName(Integer deviceId, String pointName);

    boolean existsByDeviceIdAndPointNameAndIdNot(Integer deviceId, String pointName, Integer id);
}
