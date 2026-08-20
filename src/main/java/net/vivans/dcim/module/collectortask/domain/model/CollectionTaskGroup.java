package net.vivans.dcim.module.collectortask.domain.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.shared.persistence.BaseEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(
        name = "collection_task_group",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_collection_task_group_task_cron",
                columnNames = {"task_id", "cron_expression"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CollectionTaskGroup extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private CollectionTask task;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String cronExpression;

    @Column(columnDefinition = "LONGTEXT")
    private String generatedSpec;

    @Column(length = 100)
    private String collectorJobId;

    @Column(nullable = false)
    private boolean active;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private final List<CollectionTaskDevice> devices = new ArrayList<>();

    private CollectionTaskGroup(
            CollectionTask task,
            String name,
            String cronExpression,
            boolean active
    ) {
        validateTask(task);
        validateName(name);
        validateCronExpression(cronExpression);
        this.task = task;
        this.name = name;
        this.cronExpression = cronExpression;
        this.active = active;
    }

    public static CollectionTaskGroup create(
            CollectionTask task,
            String name,
            String cronExpression,
            boolean active
    ) {
        CollectionTaskGroup group = new CollectionTaskGroup(task, name, cronExpression, active);
        task.addGroup(group);
        return group;
    }

    public void update(String name, String cronExpression, boolean active) {
        validateName(name);
        validateCronExpression(cronExpression);
        this.name = name;
        this.cronExpression = cronExpression;
        this.active = active;
    }

    public void toggleActive() {
        this.active = !this.active;
    }

    public void replaceDevices(List<Device> newDevices) {
        Set<Integer> keepIds = new HashSet<>();
        if (newDevices != null) {
            for (Device device : newDevices) {
                if (device != null && device.getId() != null) {
                    keepIds.add(device.getId());
                }
            }
        }
        devices.removeIf(mapping -> !keepIds.contains(mapping.getDevice().getId()));
        if (newDevices == null) {
            return;
        }
        for (Device device : newDevices) {
            if (device == null || device.getId() == null) {
                continue;
            }
            if (!containsDevice(device.getId())) {
                addDevice(device);
            }
        }
    }

    public void addDevice(Device device) {
        devices.add(CollectionTaskDevice.create(this, device));
    }

    public boolean containsDevice(Integer deviceId) {
        for (CollectionTaskDevice mapping : devices) {
            if (mapping.getDevice().getId().equals(deviceId)) {
                return true;
            }
        }
        return false;
    }

    public void removeDevice(Integer deviceId) {
        devices.removeIf(mapping -> mapping.getDevice().getId().equals(deviceId));
    }

    public void updateGeneratedSpec(String generatedSpec) {
        this.generatedSpec = blankToNull(generatedSpec);
    }

    public void updateCollectorJobId(String collectorJobId) {
        this.collectorJobId = blankToNull(collectorJobId);
    }

    private static void validateTask(CollectionTask task) {
        if (task == null) {
            throw new IllegalArgumentException("task is required");
        }
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
    }

    private static void validateCronExpression(String cronExpression) {
        if (cronExpression == null || cronExpression.isBlank()) {
            throw new IllegalArgumentException("cronExpression is required");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
