package net.vivans.dcim.module.device.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.shared.persistence.BaseEntity;

@Entity
@Table(name = "device_page_display_config")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DevicePageDisplayConfig extends BaseEntity {
    @Id
    @Column(name = "page_code_id")
    private Integer pageCodeId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "page_code_id")
    private CommonCode pageCode;

    @Column(name = "view_mode", nullable = false, length = 16)
    private String viewMode;

    @Column(name = "layout_asset_key", length = 120)
    private String layoutAssetKey;

    @Column(name = "location_hierarchy_initialized", nullable = false)
    private boolean locationHierarchyInitialized;

    private DevicePageDisplayConfig(CommonCode pageCode, String viewMode, String layoutAssetKey) {
        this.pageCode = pageCode;
        update(viewMode, layoutAssetKey);
    }

    public static DevicePageDisplayConfig create(CommonCode pageCode, String viewMode, String layoutAssetKey) {
        if (pageCode == null) throw new IllegalArgumentException("pageCode is required");
        return new DevicePageDisplayConfig(pageCode, viewMode, layoutAssetKey);
    }

    public void update(String viewMode, String layoutAssetKey) {
        if (viewMode == null || !java.util.Set.of("DATA", "PLAN_2D", "SCENE_3D").contains(viewMode)) {
            throw new IllegalArgumentException("viewMode must be DATA, PLAN_2D, or SCENE_3D");
        }
        this.viewMode = viewMode;
        this.layoutAssetKey = layoutAssetKey == null || layoutAssetKey.isBlank() ? null : layoutAssetKey.trim();
    }

    public boolean isLocationHierarchyInitialized() {
        return locationHierarchyInitialized;
    }

    public void markLocationHierarchyInitialized() {
        this.locationHierarchyInitialized = true;
    }
}
