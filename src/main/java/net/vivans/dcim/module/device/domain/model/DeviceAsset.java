package net.vivans.dcim.module.device.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.shared.persistence.BaseEntity;

import java.time.LocalDate;

@Entity
@Table(name = "device_asset")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeviceAsset extends BaseEntity {

    @Id
    @Column(name = "device_id")
    private Integer deviceId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(name = "asset_code", length = 100)
    private String assetCode;

    @Column(name = "serial_number", length = 200)
    private String serialNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_status_id")
    private CommonCode assetStatus;

    @Column(name = "asset_color", length = 20)
    private String assetColor;

    @Column(name = "installed_date")
    private LocalDate installedDate;

    @Column(name = "asset_manager_name", length = 100)
    private String assetManagerName;

    @Column(name = "supplier_name", length = 255)
    private String supplierName;

    @Column(name = "warranty_expires_on")
    private LocalDate warrantyExpiresOn;

    private DeviceAsset(Device device) {
        this.device = device;
        device.attachAsset(this);
    }

    public static DeviceAsset create(Device device) {
        if (device == null) throw new IllegalArgumentException("device is required");
        return new DeviceAsset(device);
    }

    public void update(String assetCode, String serialNumber, CommonCode assetStatus, String assetColor) {
        validateAssetStatus(assetStatus);
        this.assetCode = blankToNull(assetCode);
        this.serialNumber = blankToNull(serialNumber);
        this.assetStatus = assetStatus;
        this.assetColor = blankToNull(assetColor);
    }

    public void transitionStatus(CommonCode assetStatus) {
        validateAssetStatus(assetStatus);
        this.assetStatus = assetStatus;
    }

    public void updateDetails(
            LocalDate installedDate,
            String assetManagerName,
            String supplierName,
            LocalDate warrantyExpiresOn
    ) {
        this.installedDate = installedDate;
        this.assetManagerName = blankToNull(assetManagerName);
        this.supplierName = blankToNull(supplierName);
        this.warrantyExpiresOn = warrantyExpiresOn;
    }

    private static void validateAssetStatus(CommonCode assetStatus) {
        if (assetStatus == null) return;
        if (assetStatus.getCodeGroup() == null
                || !Device.ASSET_STATUS_GROUP_KEY.equals(assetStatus.getCodeGroup().getGroupKey())) {
            throw new IllegalArgumentException("assetStatus must belong to ASSET_STATUS group");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
