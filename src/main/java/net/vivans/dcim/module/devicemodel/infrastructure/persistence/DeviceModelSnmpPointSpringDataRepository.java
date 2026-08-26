package net.vivans.dcim.module.devicemodel.infrastructure.persistence;

import net.vivans.dcim.module.devicemodel.domain.model.DeviceModelSnmpPoint;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DeviceModelSnmpPointSpringDataRepository extends JpaRepository<DeviceModelSnmpPoint, Integer> {

    @EntityGraph(attributePaths = {"modelProtocol", "modelProtocol.deviceModel", "modelProtocol.protocolType"})
    Optional<DeviceModelSnmpPoint> findByIdAndModelProtocolId(Integer id, Integer modelProtocolId);

    @EntityGraph(attributePaths = {"modelProtocol", "modelProtocol.deviceModel", "modelProtocol.protocolType"})
    List<DeviceModelSnmpPoint> findAllByModelProtocolIdOrderByIdAsc(Integer modelProtocolId);

    @Query("""
            SELECT p FROM DeviceModelSnmpPoint p
            WHERE p.modelProtocol.deviceModel.id IN :deviceModelIds
              AND p.enabled = true
            """)
    @EntityGraph(attributePaths = {"modelProtocol", "modelProtocol.deviceModel"})
    List<DeviceModelSnmpPoint> findAllEnabledByDeviceModelIds(
            @Param("deviceModelIds") Collection<Integer> deviceModelIds
    );

    boolean existsByModelProtocolIdAndName(Integer modelProtocolId, String name);

    boolean existsByModelProtocolIdAndNameAndIdNot(Integer modelProtocolId, String name, Integer id);

    boolean existsByModelProtocolIdAndOid(Integer modelProtocolId, String oid);

    boolean existsByModelProtocolIdAndOidAndIdNot(Integer modelProtocolId, String oid, Integer id);

    boolean existsByModelProtocolIdAndRequiresInstanceTrue(Integer modelProtocolId);
}
