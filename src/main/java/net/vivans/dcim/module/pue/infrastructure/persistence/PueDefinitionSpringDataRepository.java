package net.vivans.dcim.module.pue.infrastructure.persistence;
import net.vivans.dcim.module.pue.domain.model.PueDefinition;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface PueDefinitionSpringDataRepository extends JpaRepository<PueDefinition, Integer> {
    @Override @EntityGraph(attributePaths = {"sources", "sources.device", "sources.device.deviceModel"}) Optional<PueDefinition> findById(Integer id);
    @EntityGraph(attributePaths = {"sources", "sources.device", "sources.device.deviceModel"}) List<PueDefinition> findAllByCollectionEnabledOrderByIdAsc(boolean collectionEnabled);
    @Override @EntityGraph(attributePaths = {"sources", "sources.device", "sources.device.deviceModel"}) List<PueDefinition> findAll();
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Integer id);
}
