package net.vivans.dcim.module.collectortask.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.shared.persistence.BaseEntity;

@Entity
@Table(
        name = "collection_task_device",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_collection_task_device_group_device",
                columnNames = {"group_id", "device_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CollectionTaskDevice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private CollectionTaskGroup group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    private CollectionTaskDevice(CollectionTaskGroup group, Device device) {
        if (group == null) {
            throw new IllegalArgumentException("group is required");
        }
        if (device == null) {
            throw new IllegalArgumentException("device is required");
        }
        this.group = group;
        this.device = device;
    }

    public static CollectionTaskDevice create(CollectionTaskGroup group, Device device) {
        return new CollectionTaskDevice(group, device);
    }
}
