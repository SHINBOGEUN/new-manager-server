package net.vivans.dcim.module.alarm.infrastructure;

import net.vivans.dcim.module.alarm.domain.AlarmEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AlarmEventRepository extends JpaRepository<AlarmEvent, Long> {
    Page<AlarmEvent> findAllByVisibility(String visibility, Pageable pageable);
    Page<AlarmEvent> findAllByVisibilityOrAlarmType(String visibility, String alarmType, Pageable pageable);
    List<AlarmEvent> findAllBySourceKey(String sourceKey);
    List<AlarmEvent> findAllBySourceKeyAndClearedDtIsNull(String sourceKey);
    Optional<AlarmEvent> findFirstBySourceKeyAndSubjectKeyAndClearedDtIsNull(String sourceKey, String subjectKey);
}
