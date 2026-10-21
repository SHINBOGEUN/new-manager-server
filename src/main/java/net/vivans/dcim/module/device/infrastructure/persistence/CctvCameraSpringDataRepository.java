package net.vivans.dcim.module.device.infrastructure.persistence;

import net.vivans.dcim.module.device.domain.model.CctvCamera;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CctvCameraSpringDataRepository extends JpaRepository<CctvCamera, Integer> {
    List<CctvCamera> findAllByEnabledTrueOrderBySortOrderAscIdAsc();
    Optional<CctvCamera> findByIdAndEnabledTrue(Integer id);
}
