package net.vivans.dcim.module.alarm.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity
@Table(name = "alarm_event")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class AlarmEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "alarm_type", nullable = false, length = 40)
    private String alarmType;
    @Column(nullable = false, length = 20)
    private String severity;
    @Column(nullable = false, length = 20)
    private String visibility;
    @Column(name = "raised_dt", nullable = false)
    private Instant raisedDt;
    @Column(name = "cleared_dt")
    private Instant clearedDt;

    @OneToOne(mappedBy = "event", fetch = FetchType.LAZY)
    private ThresholdAlarmEventDetail thresholdDetail;

    @OneToOne(mappedBy = "event", fetch = FetchType.LAZY)
    private CollectionGapAlarmDetail collectionGapDetail;

    public AlarmEvent(String alarmType, String severity, String visibility, Instant raisedDt) {
        this.alarmType = alarmType;
        this.severity = severity;
        this.visibility = visibility;
        this.raisedDt = raisedDt;
    }

    public void clear(Instant at) { this.clearedDt = at; }
    public void restrictToAdmin() { this.visibility = "ADMIN_ONLY"; }
}
