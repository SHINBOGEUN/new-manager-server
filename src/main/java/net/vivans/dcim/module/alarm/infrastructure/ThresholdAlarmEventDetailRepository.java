package net.vivans.dcim.module.alarm.infrastructure;

import net.vivans.dcim.module.alarm.domain.ThresholdAlarmEventDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ThresholdAlarmEventDetailRepository extends JpaRepository<ThresholdAlarmEventDetail, Long> {}
