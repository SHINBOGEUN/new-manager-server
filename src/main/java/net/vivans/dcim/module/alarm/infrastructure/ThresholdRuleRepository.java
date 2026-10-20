package net.vivans.dcim.module.alarm.infrastructure;

import net.vivans.dcim.module.alarm.domain.ThresholdRule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ThresholdRuleRepository extends JpaRepository<ThresholdRule, Long> {
    List<ThresholdRule> findAllByEnabledTrueOrderByIdAsc();
    boolean existsByDevice_IdAndPointName(Integer deviceId, String pointName);
    boolean existsByDevice_IdAndPointNameAndIdNot(Integer deviceId, String pointName, Long id);
    Optional<ThresholdRule> findFirstByDevice_IdAndPointName(Integer deviceId, String pointName);
}
