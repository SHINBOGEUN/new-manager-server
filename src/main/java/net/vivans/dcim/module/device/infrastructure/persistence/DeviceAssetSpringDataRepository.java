package net.vivans.dcim.module.device.infrastructure.persistence;

import net.vivans.dcim.module.device.domain.model.DeviceAsset;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceAssetSpringDataRepository extends JpaRepository<DeviceAsset, Integer> {
    boolean existsByAssetCode(String assetCode);
    boolean existsByAssetCodeAndDeviceIdNot(String assetCode, Integer deviceId);
}
