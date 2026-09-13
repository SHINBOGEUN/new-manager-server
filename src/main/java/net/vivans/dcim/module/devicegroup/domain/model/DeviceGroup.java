package net.vivans.dcim.module.devicegroup.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.shared.persistence.BaseEntity;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "device_group")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeviceGroup extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private boolean enabled;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "device_group_device",
            joinColumns = @JoinColumn(name = "device_group_id"),
            inverseJoinColumns = @JoinColumn(name = "device_id")
    )
    private Set<Device> devices = new LinkedHashSet<>();

    private DeviceGroup(String name, String description, boolean enabled) {
        validateName(name);
        this.name = name.trim();
        this.description = description;
        this.enabled = enabled;
    }

    public static DeviceGroup create(String name, String description, boolean enabled) {
        return new DeviceGroup(name, description, enabled);
    }

    public void update(String name, String description, boolean enabled, Collection<Device> devices) {
        validateName(name);
        this.name = name.trim();
        this.description = description;
        this.enabled = enabled;
        replaceDevices(devices);
    }

    public void replaceDevices(Collection<Device> devices) {
        this.devices.clear();
        if (devices != null) {
            this.devices.addAll(devices);
        }
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("device group name is required");
        }
    }
}
