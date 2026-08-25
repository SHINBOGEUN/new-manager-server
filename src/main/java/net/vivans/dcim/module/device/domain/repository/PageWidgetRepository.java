package net.vivans.dcim.module.device.domain.repository;

import net.vivans.dcim.module.device.domain.model.PageWidget;

import java.util.List;
import java.util.Optional;

public interface PageWidgetRepository {

    PageWidget save(PageWidget pageWidget);

    Optional<PageWidget> findById(Integer id);

    List<PageWidget> findAllByPageCodeIdOrderByIdAsc(Integer pageCodeId);

    boolean existsByPageCodeIdAndName(Integer pageCodeId, String name);

    boolean existsByPageCodeIdAndNameAndIdNot(Integer pageCodeId, String name, Integer id);

    void delete(PageWidget pageWidget);
}
