package net.vivans.dcim.module.device.infrastructure.persistence;

import net.vivans.dcim.module.device.domain.model.DevicePageSceneItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DevicePageSceneItemRepository extends JpaRepository<DevicePageSceneItem, Integer> {
    boolean existsByModelKey(String modelKey);
    @Query("select i from DevicePageSceneItem i left join fetch i.parent left join fetch i.device left join fetch i.locationNode " +
            "where i.pageCode.id = :pageId order by i.id")
    List<DevicePageSceneItem> findAllForPage(@Param("pageId") Integer pageId);

    @Modifying
    @Query("delete from DevicePageSceneItem i where i.pageCode.id = :pageId")
    void deleteAllForPage(@Param("pageId") Integer pageId);

    @Modifying
    @Query("delete from DevicePageSceneItem i where i.pageCode.id = :pageId and i.device.id in :deviceIds")
    void deleteAllForPageAndDevices(@Param("pageId") Integer pageId, @Param("deviceIds") List<Integer> deviceIds);
}
