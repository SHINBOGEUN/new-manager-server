package net.vivans.dcim.module.device.infrastructure.persistence;

import net.vivans.dcim.module.device.domain.model.PageWidget;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PageWidgetSpringDataRepository extends JpaRepository<PageWidget, Integer> {

    @EntityGraph(attributePaths = {
            "pageCode", "pageCode.codeGroup", "points", "layout",
            "aggregate", "count", "chart",
            "devices", "devices.device", "devices.device.deviceModel",
            "devices.device.deviceModel.deviceType", "devices.device.locationNode",
            "devices.device.pathCode",
            "models"
    })
    Optional<PageWidget> findById(Integer id);

    @EntityGraph(attributePaths = {
            "pageCode", "pageCode.codeGroup", "points", "layout",
            "aggregate", "count", "chart",
            "devices", "devices.device", "devices.device.deviceModel",
            "devices.device.deviceModel.deviceType", "devices.device.locationNode",
            "devices.device.pathCode",
            "models"
    })
    List<PageWidget> findAllByPageCode_IdOrderByIdAsc(Integer pageCodeId);

    boolean existsByPageCode_IdAndName(Integer pageCodeId, String name);

    boolean existsByPageCode_IdAndNameAndIdNot(Integer pageCodeId, String name, Integer id);
}
