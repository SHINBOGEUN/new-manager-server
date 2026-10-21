package net.vivans.dcim.module.device.domain.repository;

import net.vivans.dcim.module.device.domain.model.CctvCamera;

import java.util.List;
import java.util.Optional;

public interface CctvCameraRepository {
    List<CctvCamera> findEnabled();
    Optional<CctvCamera> findEnabledById(Integer id);
}
