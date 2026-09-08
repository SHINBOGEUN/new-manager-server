package net.vivans.dcim.module.pue.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.vivans.dcim.module.device.domain.model.Device;

@Entity
@Table(name = "pue_definition_source", uniqueConstraints =
        @UniqueConstraint(name = "uk_pue_definition_source_device", columnNames = {"pue_definition_id", "device_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PueDefinitionSource {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pue_definition_id", nullable = false)
    private PueDefinition definition;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PueDefinitionSourceRole role;

    @Column(name = "point_name", nullable = false, length = 100)
    private String pointName;

    private PueDefinitionSource(PueDefinition definition, Device device, PueDefinitionSourceRole role, String pointName) {
        if (device == null || device.getId() == null) throw new IllegalArgumentException("PUE source device is required");
        if (pointName == null || pointName.isBlank()) throw new IllegalArgumentException("PUE source pointName is required");
        this.definition = definition;
        this.device = device;
        this.role = role;
        this.pointName = pointName.trim();
    }

    static PueDefinitionSource create(PueDefinition definition, Device device, PueDefinitionSourceRole role, String pointName) {
        return new PueDefinitionSource(definition, device, role, pointName);
    }

    void update(PueDefinitionSourceRole role, String pointName) {
        if (role == null) throw new IllegalArgumentException("PUE source role is required");
        if (pointName == null || pointName.isBlank()) throw new IllegalArgumentException("PUE source pointName is required");
        this.role = role;
        this.pointName = pointName.trim();
    }
}
