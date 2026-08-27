package net.vivans.dcim.module.device.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.domain.model.PageWidget;
import net.vivans.dcim.module.device.domain.repository.PageWidgetRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PageWidgetJpaRepository implements PageWidgetRepository {

    private final PageWidgetSpringDataRepository springDataRepository;

    @Override
    public PageWidget save(PageWidget pageWidget) {
        return springDataRepository.save(pageWidget);
    }

    @Override
    public Optional<PageWidget> findById(Integer id) {
        return springDataRepository.findById(id);
    }

    @Override
    public List<PageWidget> findAllByPageCodeIdOrderByIdAsc(Integer pageCodeId) {
        return springDataRepository.findAllByPageCode_IdOrderByIdAsc(pageCodeId);
    }

    @Override
    public boolean existsByPageCodeIdAndName(Integer pageCodeId, String name) {
        return springDataRepository.existsByPageCode_IdAndName(pageCodeId, name);
    }

    @Override
    public boolean existsByPageCodeIdAndNameAndIdNot(Integer pageCodeId, String name, Integer id) {
        return springDataRepository.existsByPageCode_IdAndNameAndIdNot(pageCodeId, name, id);
    }

    @Override
    public void delete(PageWidget pageWidget) {
        springDataRepository.delete(pageWidget);
    }
}
