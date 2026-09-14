package net.vivans.dcim.module.device.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "device_image")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeviceImage extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(name = "storage_key", nullable = false, unique = true, length = 255)
    private String storageKey;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    private DeviceImage(Device device, String storageKey, String originalName, String contentType, long fileSize, int sortOrder, boolean primary) {
        this.device = device;
        this.storageKey = storageKey;
        this.originalName = originalName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.sortOrder = sortOrder;
        this.primary = primary;
    }

    public static DeviceImage create(Device device, String storageKey, String originalName, String contentType, long fileSize, int sortOrder, boolean primary) {
        if (device == null) throw new IllegalArgumentException("device is required");
        return new DeviceImage(device, storageKey, originalName, contentType, fileSize, sortOrder, primary);
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }
}
