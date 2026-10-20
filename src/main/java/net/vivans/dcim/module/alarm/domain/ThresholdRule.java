package net.vivans.dcim.module.alarm.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.device.domain.model.Device;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "threshold_rule", uniqueConstraints = @UniqueConstraint(
        name = "uk_threshold_rule_device_point", columnNames = {"device_id", "point_name"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ThresholdRule {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "protocol_type_id", nullable = false)
    private CommonCode protocolType;

    @Column(name = "point_name", nullable = false, length = 120)
    private String pointName;

    @Column(name = "operator", nullable = false, length = 10)
    private String operator;

    @Column(name = "threshold_value", nullable = false, precision = 24, scale = 8)
    private BigDecimal thresholdValue;

    @Column(name = "severity", nullable = false, length = 20)
    private String severity;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "created_dt", nullable = false, updatable = false)
    private Instant createdDt;

    @Column(name = "updated_dt", nullable = false)
    private Instant updatedDt;

    public ThresholdRule(Device device, CommonCode protocolType, String pointName,
                         String operator,
                         BigDecimal thresholdValue, String severity, boolean enabled) {
        this.device = device;
        this.protocolType = protocolType;
        this.pointName = pointName;
        this.operator = operator;
        this.thresholdValue = thresholdValue;
        this.severity = severity;
        this.enabled = enabled;
        this.createdDt = Instant.now();
        this.updatedDt = this.createdDt;
    }

    public void change(Device device, CommonCode protocolType, String pointName,
                       String operator,
                       BigDecimal thresholdValue, String severity, boolean enabled) {
        this.device = device;
        this.protocolType = protocolType;
        this.pointName = pointName;
        this.operator = operator;
        this.thresholdValue = thresholdValue;
        this.severity = severity;
        this.enabled = enabled;
        this.updatedDt = Instant.now();
    }

    public void disable() {
        this.enabled = false;
        this.updatedDt = Instant.now();
    }
}
