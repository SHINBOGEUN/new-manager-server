package net.vivans.dcim.module.alarm.infrastructure;

import net.vivans.dcim.module.alarm.domain.ThresholdRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface ThresholdRuleRepository extends JpaRepository<ThresholdRule, Long> {
    List<ThresholdRule> findAllByEnabledTrueOrderByIdAsc();
    boolean existsByDevice_IdAndPointName(Integer deviceId, String pointName);
    boolean existsByDevice_IdAndPointNameAndIdNot(Integer deviceId, String pointName, Long id);
    Optional<ThresholdRule> findFirstByDevice_IdAndPointName(Integer deviceId, String pointName);
    Optional<ThresholdRule> findFirstByDevice_IdAndPointNameAndEnabledTrue(Integer deviceId, String pointName);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ThresholdRule r where r.id = :id")
    Optional<ThresholdRule> findLockedById(@Param("id") Long id);
}
