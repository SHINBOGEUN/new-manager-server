package net.vivans.dcim.module.lora.infrastructure.persistence;

import net.vivans.dcim.module.lora.domain.model.DeviceModelLoraPoint;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface DeviceModelLoraPointSpringDataRepository extends JpaRepository<DeviceModelLoraPoint, Integer> {

    @EntityGraph(attributePaths = {"deviceModel", "dataPointType"})
    List<DeviceModelLoraPoint> findAllByDeviceModelIdOrderByIdAsc(Integer deviceModelId);

    @Query("""
            SELECT p FROM DeviceModelLoraPoint p
            WHERE p.deviceModel.id IN :deviceModelIds AND p.enabled = true
            """)
    @EntityGraph(attributePaths = {"deviceModel", "dataPointType"})
    List<DeviceModelLoraPoint> findAllEnabledByDeviceModelIdIn(@Param("deviceModelIds") Collection<Integer> deviceModelIds);

    @EntityGraph(attributePaths = {"deviceModel", "dataPointType"})
    List<DeviceModelLoraPoint> findAllByEnabledTrue();

    boolean existsByDeviceModelIdAndPayloadField(Integer deviceModelId, String payloadField);

    boolean existsByDeviceModelIdAndPayloadFieldAndIdNot(Integer deviceModelId, String payloadField, Integer id);

    boolean existsByDeviceModelIdAndPointName(Integer deviceModelId, String pointName);

    boolean existsByDeviceModelIdAndPointNameAndIdNot(Integer deviceModelId, String pointName, Integer id);
}
