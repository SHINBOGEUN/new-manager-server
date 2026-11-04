package net.vivans.dcim.module.device.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.vivans.dcim.shared.persistence.BaseEntity;

@Entity
@Table(name = "scene_model")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SceneModel extends BaseEntity {
    @Id
    @Column(name = "model_key", length = 32)
    private String key;

    @Column(name = "model_name", nullable = false, length = 120)
    private String name;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "builtin_file", length = 255)
    private String builtinFile;

    @Column(name = "storage_key", length = 255)
    private String storageKey;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "width_m", nullable = false)
    private double width;

    @Column(name = "height_m", nullable = false)
    private double height;

    @Column(name = "depth_m", nullable = false)
    private double depth;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "rack_installable", nullable = false)
    private boolean rackInstallable;

    @Column(name = "rack_u_installable", nullable = false)
    private boolean rackUInstallable;
    @Column(name = "rack_attachment_installable", nullable = false)
    private boolean rackAttachmentInstallable;

    @Column(name = "device_linkable", nullable = false)
    private boolean deviceLinkable;

    public void setDeviceLinkable(boolean allowed) { this.deviceLinkable = allowed; }

    public void setRackInstallable(boolean allowed) { this.rackInstallable = allowed; this.rackUInstallable = allowed; this.rackAttachmentInstallable = allowed; }
    public void setRackMountOptions(boolean rackU, boolean attachment) { this.rackUInstallable = rackU; this.rackAttachmentInstallable = attachment; this.rackInstallable = rackU || attachment; }
    public boolean permitsRackMount(DeviceMountType type) {
        return isRackInstallable() && (type == DeviceMountType.RACK_U ? rackUInstallable : (type == DeviceMountType.RACK_SIDE || type == DeviceMountType.RACK_REAR) && rackAttachmentInstallable);
    }

    public static SceneModel uploaded(String key, String name, String category, String storageKey, String fileName,
                                      double width, double height, double depth) {
        SceneModel model = new SceneModel();
        model.key = key;
        model.storageKey = storageKey;
        model.fileName = fileName;
        model.enabled = true;
        model.update(name, category, width, height, depth, true);
        return model;
    }

    public void update(String name, String category, double width, double height, double depth, boolean enabled) {
        if (name == null || name.isBlank() || name.trim().length() > 120) {
            throw new IllegalArgumentException("모델 이름은 1~120자여야 합니다.");
        }
        if (category == null || category.isBlank() || category.trim().length() > 50) {
            throw new IllegalArgumentException("카테고리는 1~50자여야 합니다.");
        }
        if (!validDimension(width) || !validDimension(height) || !validDimension(depth)) {
            throw new IllegalArgumentException("모델 크기는 각 축 0.01~100m여야 합니다.");
        }
        this.name = name.trim();
        this.category = category.trim();
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.enabled = enabled;
    }

    private static boolean validDimension(double value) {
        return Double.isFinite(value) && value >= .01 && value <= 100;
    }

    public void replaceFile(String storageKey, String fileName) {
        if (storageKey == null || storageKey.isBlank()) throw new IllegalArgumentException("storageKey is required");
        this.storageKey = storageKey;
        this.fileName = fileName;
        this.builtinFile = null;
    }

    public boolean isBuiltin() { return builtinFile != null && storageKey == null; }
}
