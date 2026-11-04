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
@Table(name = "scene_builtin_asset")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SceneBuiltinAsset extends BaseEntity {
    @Id
    @Column(name = "asset_key", length = 32)
    private String key;

    @Column(name = "asset_name", nullable = false, length = 120)
    private String label;

    @Column(name = "category", nullable = false, length = 20)
    private String category;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    public void update(String label, boolean enabled) {
        if (label == null || label.isBlank() || label.trim().length() > 120) {
            throw new IllegalArgumentException("자산 이름은 1~120자여야 합니다.");
        }
        this.label = label.trim();
        this.enabled = enabled;
    }
}
