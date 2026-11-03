package net.vivans.dcim.module.device.infrastructure.persistence;

import net.vivans.dcim.module.device.domain.model.RackModelPlacement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface RackModelPlacementRepository extends JpaRepository<RackModelPlacement, Integer> {
    List<RackModelPlacement> findAllByRackLocationCode(String code);
    boolean existsBySceneModelKey(String key);
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM RackModelPlacement p WHERE p.rackLocationCode = :code AND p.uPosition <= :endU AND (p.uPosition + p.uHeight - 1) >= :startU")
    boolean overlaps(@Param("code") String code, @Param("startU") int startU, @Param("endU") int endU);
}
