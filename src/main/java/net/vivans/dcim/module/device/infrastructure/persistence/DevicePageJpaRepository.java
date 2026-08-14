package net.vivans.dcim.module.device.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.domain.model.DevicePage;
import net.vivans.dcim.module.device.domain.repository.DevicePageRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DevicePageJpaRepository implements DevicePageRepository {

    private final DevicePageSpringDataRepository springDataRepository;

    @Override
    public DevicePage save(DevicePage devicePage) {
        return springDataRepository.save(devicePage);
    }

    @Override
    public List<DevicePage> saveAll(Iterable<DevicePage> devicePages) {
        return springDataRepository.saveAll(devicePages);
    }

    @Override
    public Optional<DevicePage> findByIdAndDeviceId(Integer id, Integer deviceId) {
        return springDataRepository.findByIdAndDevice_Id(id, deviceId);
    }

    @Override
    public List<DevicePage> findAllByDeviceIdOrderByIdAsc(Integer deviceId) {
        return springDataRepository.findAllByDevice_IdOrderByIdAsc(deviceId);
    }

    @Override
    public boolean existsByDeviceIdAndPageCodeId(Integer deviceId, Integer pageCodeId) {
        return springDataRepository.existsByDevice_IdAndPageCode_Id(deviceId, pageCodeId);
    }

    @Override
    public void delete(DevicePage devicePage) {
        springDataRepository.delete(devicePage);
    }

    @Override
    public void deleteAll(Iterable<DevicePage> devicePages) {
        springDataRepository.deleteAll(devicePages);
    }
}
