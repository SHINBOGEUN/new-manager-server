package net.vivans.dcim.module.device.infrastructure.persistence;

import net.vivans.dcim.module.device.domain.model.DeviceRackPlacement;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DeviceRackPlacementSpringDataRepository extends JpaRepository<DeviceRackPlacement, Integer> {
    @EntityGraph(attributePaths = {"device", "device.deviceModel", "rackLocation"})
    Optional<DeviceRackPlacement> findByDevice_Id(Integer deviceId);

    @EntityGraph(attributePaths = {"device", "device.deviceModel", "rackLocation"})
    @Query("SELECT p FROM DeviceRackPlacement p WHERE p.rackLocation.code = :rackLocationCode ORDER BY p.uPosition ASC")
    List<DeviceRackPlacement> findAllByRackLocationCode(@Param("rackLocationCode") String rackLocationCode);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM DeviceRackPlacement p " +
            "WHERE p.rackLocation.code = :rackLocationCode " +
            "AND p.uPosition <= :endU " +
            "AND (p.uPosition + p.uHeight - 1) >= :startU " +
            "AND (:excludeDeviceId IS NULL OR p.device.id <> :excludeDeviceId)")
    boolean existsOverlapping(
            @Param("rackLocationCode") String rackLocationCode,
            @Param("startU") int startU,
            @Param("endU") int endU,
            @Param("excludeDeviceId") Integer excludeDeviceId);
}
