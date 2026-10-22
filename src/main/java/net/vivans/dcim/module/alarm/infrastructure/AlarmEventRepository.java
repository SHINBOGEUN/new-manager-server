package net.vivans.dcim.module.alarm.infrastructure;

import net.vivans.dcim.module.alarm.domain.AlarmEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

public interface AlarmEventRepository extends JpaRepository<AlarmEvent, Long>, JpaSpecificationExecutor<AlarmEvent> {
    Page<AlarmEvent> findAllByVisibility(String visibility, Pageable pageable);
    List<AlarmEvent> findByIdGreaterThanOrderByIdAsc(Long afterId, Pageable pageable);

    @Query("select e from AlarmEvent e join e.thresholdDetail d where d.thresholdRule.id = :ruleId and d.device.id = :deviceId and e.clearedDt is null")
    Optional<AlarmEvent> findOpenThresholdEvent(@Param("ruleId") Long ruleId, @Param("deviceId") Integer deviceId);

    @Query("select e from AlarmEvent e join e.thresholdDetail d where d.thresholdRule.id = :ruleId and e.clearedDt is null")
    List<AlarmEvent> findOpenThresholdEvents(@Param("ruleId") Long ruleId);

    @Query("select e from AlarmEvent e where e.id > :afterId and e.visibility = 'USER_VISIBLE' order by e.id asc")
    List<AlarmEvent> findVisibleAfter(@Param("afterId") Long afterId, Pageable pageable);

    @Query("select coalesce(max(e.id), 0) from AlarmEvent e")
    Long findLatestId();

    @Query("select coalesce(max(e.id), 0) from AlarmEvent e where e.visibility = 'USER_VISIBLE'")
    Long findLatestVisibleId();

    @Query("select count(e) from AlarmEvent e where e.clearedDt is null and (:includeAdmin = true or e.visibility = 'USER_VISIBLE')")
    long countOpenForSummary(@Param("includeAdmin") boolean includeAdmin);

    @Query("select e.severity, count(e) from AlarmEvent e where e.clearedDt is null and (:includeAdmin = true or e.visibility = 'USER_VISIBLE') group by e.severity")
    List<Object[]> countOpenBySeverityForSummary(@Param("includeAdmin") boolean includeAdmin);

    @Query("select e from AlarmEvent e where e.clearedDt is null and (:includeAdmin = true or e.visibility = 'USER_VISIBLE') order by e.raisedDt desc, e.id desc")
    List<AlarmEvent> findLatestOpenForSummary(@Param("includeAdmin") boolean includeAdmin, Pageable pageable);

}
