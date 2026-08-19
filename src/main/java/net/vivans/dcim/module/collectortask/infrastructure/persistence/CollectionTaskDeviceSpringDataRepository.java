package net.vivans.dcim.module.collectortask.infrastructure.persistence;

import net.vivans.dcim.module.collectortask.domain.model.CollectionTaskDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

public interface CollectionTaskDeviceSpringDataRepository extends JpaRepository<CollectionTaskDevice, Integer> {

    @Modifying(clearAutomatically = true)
    void deleteByDeviceId(Integer deviceId);
}
