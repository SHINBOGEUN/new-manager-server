package net.vivans.dcim.module.device.infrastructure.persistence;

import net.vivans.dcim.module.device.domain.model.DevicePageDevice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DevicePageDeviceRepository extends JpaRepository<DevicePageDevice, Integer> {
    @EntityGraph(attributePaths = {"device", "device.deviceModel", "device.locationNode", "device.locationNode.locationType"})
    List<DevicePageDevice> findAllByPageCode_IdOrderByIdAsc(Integer pageCodeId);
}
