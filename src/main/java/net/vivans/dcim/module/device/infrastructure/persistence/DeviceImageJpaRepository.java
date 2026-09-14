package net.vivans.dcim.module.device.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.domain.model.DeviceImage;
import net.vivans.dcim.module.device.domain.repository.DeviceImageRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DeviceImageJpaRepository implements DeviceImageRepository {
    private final DeviceImageSpringDataRepository springDataRepository;
    public DeviceImage save(DeviceImage image) { return springDataRepository.save(image); }
    public List<DeviceImage> saveAll(Iterable<DeviceImage> images) { return springDataRepository.saveAll(images); }
    public List<DeviceImage> findAllByDeviceId(Integer deviceId) { return springDataRepository.findAllByDevice_IdOrderByPrimaryDescSortOrderAscIdAsc(deviceId); }
    public Optional<DeviceImage> findByIdAndDeviceId(Integer imageId, Integer deviceId) { return springDataRepository.findByIdAndDevice_Id(imageId, deviceId); }
    public void delete(DeviceImage image) { springDataRepository.delete(image); }
}
