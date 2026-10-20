package net.vivans.dcim.module.alarm.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.vivans.dcim.module.device.domain.model.Device;

import java.math.BigDecimal;

@Entity
@Table(name = "threshold_alarm_event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ThresholdAlarmEventDetail {
    @Id
    @Column(name = "event_id")
    private Long eventId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "event_id", nullable = false)
    private AlarmEvent event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "threshold_rule_id", nullable = false)
    private ThresholdRule thresholdRule;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(name = "point_name", nullable = false, length = 120)
    private String pointName;

    @Column(name = "unit", nullable = false, length = 40)
    private String unit;

    @Column(name = "operator", nullable = false, length = 10)
    private String operator;

    @Column(name = "threshold_value", nullable = false, precision = 24, scale = 8)
    private BigDecimal thresholdValue;

    @Column(name = "measured_value", nullable = false, precision = 24, scale = 8)
    private BigDecimal measuredValue;

    public ThresholdAlarmEventDetail(AlarmEvent event, ThresholdRule thresholdRule, Device device,
                                     String pointName, String unit, String operator,
                                     BigDecimal thresholdValue, BigDecimal measuredValue) {
        this.event = event;
        this.thresholdRule = thresholdRule;
        this.device = device;
        this.pointName = pointName;
        this.unit = unit;
        this.operator = operator;
        this.thresholdValue = thresholdValue;
        this.measuredValue = measuredValue;
    }
}
