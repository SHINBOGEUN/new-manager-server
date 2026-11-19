package net.vivans.dcim.module.collectortask.domain.repository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import net.vivans.dcim.module.collectortask.domain.model.DeviceCollectionSetting;
public interface DeviceCollectionSettingRepository extends JpaRepository<DeviceCollectionSetting,Integer> {
    List<DeviceCollectionSetting> findAllByDeviceIdOrderByIdAsc(Integer deviceId);
    Optional<DeviceCollectionSetting> findByDeviceIdAndProtocolTypeId(Integer deviceId,Integer protocolTypeId);
    @Query("select distinct s.device.id from DeviceCollectionSetting s order by s.device.id") List<Integer> deviceIds();
}
