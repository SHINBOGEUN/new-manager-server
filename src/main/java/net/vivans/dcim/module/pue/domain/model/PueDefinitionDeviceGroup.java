package net.vivans.dcim.module.pue.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.vivans.dcim.module.devicegroup.domain.model.DeviceGroup;

@Entity
@Table(name = "pue_definition_device_group", uniqueConstraints =
        @UniqueConstraint(name = "uk_pue_definition_device_group", columnNames = {"pue_definition_id", "device_group_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PueDefinitionDeviceGroup {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pue_definition_id", nullable = false)
    private PueDefinition definition;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_group_id", nullable = false)
    private DeviceGroup deviceGroup;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PueDefinitionSourceRole role;

    @Column(name = "point_name", nullable = false, length = 100)
    private String pointName;

    private PueDefinitionDeviceGroup(PueDefinition definition, DeviceGroup deviceGroup,
                                     PueDefinitionSourceRole role, String pointName) {
        if (deviceGroup == null || deviceGroup.getId() == null) {
            throw new IllegalArgumentException("PUE device group is required");
        }
        if (role == null) throw new IllegalArgumentException("PUE device group role is required");
        if (pointName == null || pointName.isBlank()) {
            throw new IllegalArgumentException("PUE device group pointName is required");
        }
        this.definition = definition;
        this.deviceGroup = deviceGroup;
        this.role = role;
        this.pointName = pointName.trim();
    }

    static PueDefinitionDeviceGroup create(PueDefinition definition, DeviceGroup deviceGroup,
                                           PueDefinitionSourceRole role, String pointName) {
        return new PueDefinitionDeviceGroup(definition, deviceGroup, role, pointName);
    }

    void update(PueDefinitionSourceRole role, String pointName) {
        if (role == null) throw new IllegalArgumentException("PUE device group role is required");
        if (pointName == null || pointName.isBlank()) {
            throw new IllegalArgumentException("PUE device group pointName is required");
        }
        this.role = role;
        this.pointName = pointName.trim();
    }
}
