package net.vivans.dcim.module.device.infrastructure.persistence;

import net.vivans.dcim.module.device.domain.model.SceneModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SceneModelRepository extends JpaRepository<SceneModel, String> {
    java.util.List<SceneModel> findAllByDeviceLinkableTrue();
}
