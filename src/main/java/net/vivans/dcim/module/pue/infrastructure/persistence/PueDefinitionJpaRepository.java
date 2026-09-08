package net.vivans.dcim.module.pue.infrastructure.persistence;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.pue.domain.model.PueDefinition;
import net.vivans.dcim.module.pue.domain.repository.PueDefinitionRepository;
import org.springframework.stereotype.Repository;
import java.util.*;
@Repository @RequiredArgsConstructor
public class PueDefinitionJpaRepository implements PueDefinitionRepository {
 private final PueDefinitionSpringDataRepository repository;
 public PueDefinition save(PueDefinition d){return repository.save(d);} public Optional<PueDefinition> findById(Integer id){return repository.findById(id);}
 public List<PueDefinition> findAllByCollectionEnabled(boolean enabled){return repository.findAllByCollectionEnabledOrderByIdAsc(enabled);}
 public List<PueDefinition> findAll(){return repository.findAll();} public boolean existsByName(String n){return repository.existsByName(n);}
 public boolean existsByNameAndIdNot(String n,Integer id){return repository.existsByNameAndIdNot(n,id);} public void delete(PueDefinition d){repository.delete(d);}
}
