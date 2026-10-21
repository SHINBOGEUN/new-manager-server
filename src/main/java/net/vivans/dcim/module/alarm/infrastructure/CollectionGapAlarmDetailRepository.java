package net.vivans.dcim.module.alarm.infrastructure;

import net.vivans.dcim.module.alarm.domain.CollectionGapAlarmDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CollectionGapAlarmDetailRepository extends JpaRepository<CollectionGapAlarmDetail, Long> {
    @Query("select d from CollectionGapAlarmDetail d where d.event.clearedDt is null")
    List<CollectionGapAlarmDetail> findAllOpen();
}
