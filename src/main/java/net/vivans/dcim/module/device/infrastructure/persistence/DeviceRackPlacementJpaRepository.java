package net.vivans.dcim.module.device.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.domain.model.DeviceRackPlacement;
import net.vivans.dcim.module.device.domain.repository.DeviceRackPlacementRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DeviceRackPlacementJpaRepository implements DeviceRackPlacementRepository {
    private final DeviceRackPlacementSpringDataRepository springDataRepository;
    public DeviceRackPlacement save(DeviceRackPlacement placement) { return springDataRepository.save(placement); }
    public Optional<DeviceRackPlacement> findByDeviceId(Integer deviceId) { return springDataRepository.findByDevice_Id(deviceId); }
    public List<DeviceRackPlacement> findAllByRackLocationCode(String rackLocationCode) { return springDataRepository.findAllByRackLocationCode(rackLocationCode); }
    public boolean existsOverlapping(String rackLocationCode, int startU, int endU, Integer excludeDeviceId) { return springDataRepository.existsOverlapping(rackLocationCode, startU, endU, excludeDeviceId); }
    public void delete(DeviceRackPlacement placement) { springDataRepository.delete(placement); }
}
