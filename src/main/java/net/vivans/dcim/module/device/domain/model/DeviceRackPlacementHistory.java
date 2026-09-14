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
import net.vivans.dcim.shared.persistence.BaseEntity;

@Entity
@Table(name = "device_rack_placement_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeviceRackPlacementHistory extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeviceRackPlacementHistoryAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_mount_type", length = 30)
    private DeviceMountType previousMountType;

    @Column(name = "previous_rack_location_code", length = 10)
    private String previousRackLocationCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_rack_side", length = 20)
    private DeviceRackSide previousRackSide;

    @Column(name = "previous_u_position")
    private Integer previousUPosition;

    @Column(name = "previous_u_height")
    private Integer previousUHeight;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_mount_type", length = 30)
    private DeviceMountType currentMountType;

    @Column(name = "current_rack_location_code", length = 10)
    private String currentRackLocationCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_rack_side", length = 20)
    private DeviceRackSide currentRackSide;

    @Column(name = "current_u_position")
    private Integer currentUPosition;

    @Column(name = "current_u_height")
    private Integer currentUHeight;

    private DeviceRackPlacementHistory(Device device, DeviceRackPlacementHistoryAction action,
                                       PlacementSnapshot previous, PlacementSnapshot current) {
        this.device = device;
        this.action = action;
        applyPrevious(previous);
        applyCurrent(current);
    }

    public static DeviceRackPlacementHistory create(Device device, DeviceRackPlacementHistoryAction action,
                                                    PlacementSnapshot previous, PlacementSnapshot current) {
        return new DeviceRackPlacementHistory(device, action, previous, current);
    }

    private void applyPrevious(PlacementSnapshot snapshot) {
        if (snapshot == null) return;
        previousMountType = snapshot.mountType();
        previousRackLocationCode = snapshot.rackLocationCode();
        previousRackSide = snapshot.rackSide();
        previousUPosition = snapshot.uPosition();
        previousUHeight = snapshot.uHeight();
    }

    private void applyCurrent(PlacementSnapshot snapshot) {
        if (snapshot == null) return;
        currentMountType = snapshot.mountType();
        currentRackLocationCode = snapshot.rackLocationCode();
        currentRackSide = snapshot.rackSide();
        currentUPosition = snapshot.uPosition();
        currentUHeight = snapshot.uHeight();
    }

    public record PlacementSnapshot(DeviceMountType mountType, String rackLocationCode, DeviceRackSide rackSide,
                                    Integer uPosition, Integer uHeight) {
        public static PlacementSnapshot from(DeviceRackPlacement placement) {
            if (placement == null) return null;
            return new PlacementSnapshot(placement.getMountType(),
                    placement.getRackLocation() == null ? null : placement.getRackLocation().getCode(),
                    placement.getRackSide(), placement.getUPosition(), placement.getUHeight());
        }
    }
}
