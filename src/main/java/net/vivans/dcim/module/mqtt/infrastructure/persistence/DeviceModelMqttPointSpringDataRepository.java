package net.vivans.dcim.module.mqtt.infrastructure.persistence;

import net.vivans.dcim.module.mqtt.domain.model.DeviceModelMqttPoint;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface DeviceModelMqttPointSpringDataRepository extends JpaRepository<DeviceModelMqttPoint, Integer> {

    @EntityGraph(attributePaths = {"deviceModel", "dataPointType", "unitCode", "categoryCode"})
    List<DeviceModelMqttPoint> findAllByDeviceModelIdOrderByIdAsc(Integer deviceModelId);

    @Query("""
            SELECT p FROM DeviceModelMqttPoint p
            WHERE p.deviceModel.id IN :deviceModelIds AND p.enabled = true
            """)
    @EntityGraph(attributePaths = {"deviceModel", "dataPointType", "unitCode", "categoryCode"})
    List<DeviceModelMqttPoint> findAllEnabledByDeviceModelIdIn(@Param("deviceModelIds") Collection<Integer> deviceModelIds);

    @EntityGraph(attributePaths = {"deviceModel", "dataPointType", "unitCode", "categoryCode"})
    List<DeviceModelMqttPoint> findAllByEnabledTrue();

    boolean existsByDeviceModelIdAndPayloadField(Integer deviceModelId, String payloadField);

    boolean existsByDeviceModelIdAndPayloadFieldAndIdNot(Integer deviceModelId, String payloadField, Integer id);

    boolean existsByDeviceModelIdAndPointName(Integer deviceModelId, String pointName);

    boolean existsByDeviceModelIdAndPointNameAndIdNot(Integer deviceModelId, String pointName, Integer id);
}
