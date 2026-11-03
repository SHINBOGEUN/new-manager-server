package net.vivans.dcim.module.device.infrastructure.persistence;

import net.vivans.dcim.module.device.domain.model.SceneBuiltinAsset;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SceneBuiltinAssetRepository extends JpaRepository<SceneBuiltinAsset, String> {
}
