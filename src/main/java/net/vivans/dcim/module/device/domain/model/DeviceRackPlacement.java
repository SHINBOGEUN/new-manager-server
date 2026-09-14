package net.vivans.dcim.module.device.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.vivans.dcim.module.location.domain.model.LocationNode;
import net.vivans.dcim.shared.persistence.BaseEntity;

@Entity
@Table(name = "device_rack_placement", uniqueConstraints = @UniqueConstraint(
        name = "uk_device_rack_placement_device", columnNames = "device_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeviceRackPlacement extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rack_location_code")
    private LocationNode rackLocation;

    @Enumerated(EnumType.STRING)
    @Column(name = "mount_type", nullable = false, length = 30)
    private DeviceMountType mountType;

    @Enumerated(EnumType.STRING)
    @Column(name = "rack_side", length = 20)
    private DeviceRackSide rackSide;

    @Column(name = "u_position")
    private Integer uPosition;

    @Column(name = "u_height")
    private Integer uHeight;

    @Column(name = "form_factor", length = 30)
    private String formFactor;

    private DeviceRackPlacement(Device device, LocationNode rackLocation, DeviceMountType mountType, DeviceRackSide rackSide, Integer uPosition, Integer uHeight, String formFactor) {
        update(device, rackLocation, mountType, rackSide, uPosition, uHeight, formFactor);
    }

    public static DeviceRackPlacement create(Device device, LocationNode rackLocation, DeviceMountType mountType, DeviceRackSide rackSide, Integer uPosition, Integer uHeight, String formFactor) {
        return new DeviceRackPlacement(device, rackLocation, mountType, rackSide, uPosition, uHeight, formFactor);
    }

    public void update(Device device, LocationNode rackLocation, DeviceMountType mountType, DeviceRackSide rackSide, Integer uPosition, Integer uHeight, String formFactor) {
        if (device == null) throw new IllegalArgumentException("device is required");
        if (mountType == null) throw new IllegalArgumentException("mountType is required");
        if (mountType.requiresRack() && rackLocation == null) throw new IllegalArgumentException("rackLocation is required");
        if (mountType.usesRackU() && (uPosition == null || uPosition < 1 || uHeight == null || uHeight < 1)) {
            throw new IllegalArgumentException("RACK_U requires uPosition and uHeight");
        }
        if (mountType == DeviceMountType.RACK_SIDE && (rackSide == null || rackSide == DeviceRackSide.REAR)) {
            throw new IllegalArgumentException("RACK_SIDE requires LEFT or RIGHT rackSide");
        }
        if (mountType == DeviceMountType.RACK_REAR && rackSide != null && rackSide != DeviceRackSide.REAR) {
            throw new IllegalArgumentException("RACK_REAR only supports REAR rackSide");
        }
        this.device = device;
        this.rackLocation = rackLocation;
        this.mountType = mountType;
        this.rackSide = mountType == DeviceMountType.RACK_SIDE ? rackSide
                : mountType == DeviceMountType.RACK_REAR ? DeviceRackSide.REAR : null;
        this.uPosition = mountType.usesRackU() ? uPosition : null;
        this.uHeight = mountType.usesRackU() ? uHeight : null;
        this.formFactor = formFactor == null || formFactor.isBlank()
                ? mountType.usesRackU() ? uHeight + "U" : null : formFactor.trim();
    }

    public Integer lastU() {
        return !mountType.usesRackU() ? null : uPosition + uHeight - 1;
    }
}
