package net.vivans.dcim.module.device.infrastructure.persistence;

import net.vivans.dcim.module.device.domain.model.DevicePageDisplayConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DevicePageDisplayConfigRepository extends JpaRepository<DevicePageDisplayConfig, Integer> {
}
