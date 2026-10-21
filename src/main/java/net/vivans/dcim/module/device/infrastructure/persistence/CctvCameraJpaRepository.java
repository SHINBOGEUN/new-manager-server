package net.vivans.dcim.module.device.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.domain.model.CctvCamera;
import net.vivans.dcim.module.device.domain.repository.CctvCameraRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CctvCameraJpaRepository implements CctvCameraRepository {
    private final CctvCameraSpringDataRepository repository;

    @Override
    public List<CctvCamera> findEnabled() {
        return repository.findAllByEnabledTrueOrderBySortOrderAscIdAsc();
    }

    @Override
    public Optional<CctvCamera> findEnabledById(Integer id) {
        return repository.findByIdAndEnabledTrue(id);
    }
}
