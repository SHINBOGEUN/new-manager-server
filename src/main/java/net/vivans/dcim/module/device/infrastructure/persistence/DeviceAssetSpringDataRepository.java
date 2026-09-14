package net.vivans.dcim.module.device.infrastructure.persistence;

import net.vivans.dcim.module.device.domain.model.DeviceAsset;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeviceAssetSpringDataRepository extends JpaRepository<DeviceAsset, Integer> {
    boolean existsByAssetCode(String assetCode);
    boolean existsByAssetCodeAndDeviceIdNot(String assetCode, Integer deviceId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM DeviceAsset asset WHERE asset.deviceId = :deviceId")
    void deleteDirectlyByDeviceId(@Param("deviceId") Integer deviceId);
}
