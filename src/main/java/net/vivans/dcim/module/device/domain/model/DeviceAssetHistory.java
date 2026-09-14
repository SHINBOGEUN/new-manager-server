package net.vivans.dcim.module.device.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.shared.persistence.BaseEntity;

@Entity
@Table(name = "device_asset_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeviceAssetHistory extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DeviceAssetHistoryAction action;

    @Column(name = "actor_name", length = 100)
    private String actorName;

    @Column(length = 1000)
    private String reason;

    @Column(name = "previous_asset_code", length = 100)
    private String previousAssetCode;

    @Column(name = "current_asset_code", length = 100)
    private String currentAssetCode;

    @Column(name = "previous_serial_number", length = 200)
    private String previousSerialNumber;

    @Column(name = "current_serial_number", length = 200)
    private String currentSerialNumber;

    @Column(name = "previous_status_code", length = 100)
    private String previousStatusCode;

    @Column(name = "previous_status_name", length = 255)
    private String previousStatusName;

    @Column(name = "current_status_code", length = 100)
    private String currentStatusCode;

    @Column(name = "current_status_name", length = 255)
    private String currentStatusName;

    @Column(name = "previous_enabled")
    private Boolean previousEnabled;

    @Column(name = "current_enabled")
    private Boolean currentEnabled;

    private DeviceAssetHistory(Device device, DeviceAssetHistoryAction action, String actorName,
                               String reason, AssetSnapshot previous, AssetSnapshot current) {
        this.device = device;
        this.action = action;
        this.actorName = blankToNull(actorName);
        this.reason = blankToNull(reason);
        applyPrevious(previous);
        applyCurrent(current);
    }

    public static DeviceAssetHistory create(Device device, DeviceAssetHistoryAction action, String actorName,
                                            String reason, AssetSnapshot previous, AssetSnapshot current) {
        return new DeviceAssetHistory(device, action, actorName, reason, previous, current);
    }

    private void applyPrevious(AssetSnapshot snapshot) {
        if (snapshot == null) return;
        previousAssetCode = snapshot.assetCode();
        previousSerialNumber = snapshot.serialNumber();
        previousStatusCode = snapshot.statusCode();
        previousStatusName = snapshot.statusName();
        previousEnabled = snapshot.enabled();
    }

    private void applyCurrent(AssetSnapshot snapshot) {
        if (snapshot == null) return;
        currentAssetCode = snapshot.assetCode();
        currentSerialNumber = snapshot.serialNumber();
        currentStatusCode = snapshot.statusCode();
        currentStatusName = snapshot.statusName();
        currentEnabled = snapshot.enabled();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record AssetSnapshot(String assetCode, String serialNumber, String statusCode,
                                String statusName, boolean enabled) {
        public static AssetSnapshot from(Device device, DeviceAsset asset) {
            CommonCode status = asset == null ? null : asset.getAssetStatus();
            return new AssetSnapshot(asset == null ? null : asset.getAssetCode(), asset == null ? null : asset.getSerialNumber(),
                    status == null ? null : status.getCode(), status == null ? null : status.getName(),
                    device.isEnabled());
        }
    }
}
