package net.vivans.dcim.module.devicemodel.infrastructure.persistence;

import net.vivans.dcim.module.devicemodel.domain.model.DeviceModelModbusPoint;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DeviceModelModbusPointSpringDataRepository extends JpaRepository<DeviceModelModbusPoint, Integer> {

    @EntityGraph(attributePaths = {"modelProtocol", "modelProtocol.deviceModel", "modelProtocol.protocolType"})
    Optional<DeviceModelModbusPoint> findByIdAndModelProtocolId(Integer id, Integer modelProtocolId);

    @EntityGraph(attributePaths = {"modelProtocol", "modelProtocol.deviceModel", "modelProtocol.protocolType"})
    List<DeviceModelModbusPoint> findAllByModelProtocolIdOrderByIdAsc(Integer modelProtocolId);

    @Query("""
            SELECT p FROM DeviceModelModbusPoint p
            WHERE p.modelProtocol.deviceModel.id IN :deviceModelIds
              AND p.enabled = true
            """)
    @EntityGraph(attributePaths = {"modelProtocol", "modelProtocol.deviceModel"})
    List<DeviceModelModbusPoint> findAllEnabledByDeviceModelIds(
            @Param("deviceModelIds") Collection<Integer> deviceModelIds
    );

    boolean existsByModelProtocolIdAndName(Integer modelProtocolId, String name);

    boolean existsByModelProtocolIdAndNameAndIdNot(Integer modelProtocolId, String name, Integer id);
}
