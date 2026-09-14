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
@Table(name = "device_asset_document")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeviceAssetDocument extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(name = "storage_key", nullable = false, unique = true, length = 255)
    private String storageKey;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "content_type", nullable = false, length = 150)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    private DeviceAssetDocument(Device device, String storageKey, String originalName, String contentType, long fileSize) {
        this.device = device;
        this.storageKey = storageKey;
        this.originalName = originalName;
        this.contentType = contentType;
        this.fileSize = fileSize;
    }

    public static DeviceAssetDocument create(Device device, String storageKey, String originalName, String contentType, long fileSize) {
        if (device == null) throw new IllegalArgumentException("device is required");
        return new DeviceAssetDocument(device, storageKey, originalName, contentType, fileSize);
    }
}
