package net.vivans.dcim.module.device.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.shared.persistence.BaseEntity;

/** Per-device collection monitoring policy. */
@Entity
@Table(name = "device_monitoring_policy")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeviceMonitoringPolicy extends BaseEntity {
    @Id
    @Column(name = "device_id")
    private Integer deviceId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "monitoring_mode_code_id", nullable = false)
    private CommonCode monitoringMode;

    private DeviceMonitoringPolicy(Integer deviceId, CommonCode monitoringMode) {
        update(deviceId, monitoringMode);
    }

    public static DeviceMonitoringPolicy create(Integer deviceId, CommonCode monitoringMode) {
        return new DeviceMonitoringPolicy(deviceId, monitoringMode);
    }

    public void update(Integer deviceId, CommonCode monitoringMode) {
        if (deviceId == null || deviceId < 1) throw new IllegalArgumentException("deviceId is required");
        if (monitoringMode == null || monitoringMode.getCodeGroup() == null
                || !"DEVICE_MONITORING_MODE".equals(monitoringMode.getCodeGroup().getGroupKey())) {
            throw new IllegalArgumentException("유효한 장비 감시 방식을 선택하세요");
        }
        String mode = monitoringMode.getCode();
        if (!"PERIODIC".equals(mode) && !"ON_CHANGE".equals(mode)) {
            throw new IllegalArgumentException("지원하지 않는 장비 감시 방식입니다");
        }
        this.deviceId = deviceId;
        this.monitoringMode = monitoringMode;
    }
}
