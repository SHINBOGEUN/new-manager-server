package net.vivans.dcim.module.pue.domain.repository;
import net.vivans.dcim.module.pue.domain.model.PueDefinition;
import java.util.*;
public interface PueDefinitionRepository {
    PueDefinition save(PueDefinition definition);
    Optional<PueDefinition> findById(Integer id);
    List<PueDefinition> findAllByCollectionEnabled(boolean enabled);
    List<PueDefinition> findAll();
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Integer id);
    void delete(PueDefinition definition);
}
