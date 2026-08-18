package net.vivans.dcim.module.device.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import net.vivans.dcim.module.location.domain.model.LocationNode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DeviceJpaRepository implements DeviceRepository {

    private final DeviceSpringDataRepository springDataRepository;

    @Override
    public Device save(Device device) {
        return springDataRepository.save(device);
    }

    @Override
    public List<Device> saveAll(Iterable<Device> devices) {
        return springDataRepository.saveAll(devices);
    }

    @Override
    public Optional<Device> findById(Integer id) {
        return springDataRepository.findById(id);
    }

    @Override
    public List<Device> findByLocationNodeCode(String locationNodeCode) {
        return springDataRepository.findByLocationNode_Code(locationNodeCode);
    }

    @Override
    public List<Device> findByLocationNodeCodeIn(Collection<String> locationNodeCodes) {
        if (locationNodeCodes == null || locationNodeCodes.isEmpty()) {
            return List.of();
        }
        return springDataRepository.findByLocationNode_CodeIn(locationNodeCodes);
    }

    @Override
    public Page<Device> findAll(
            Integer modelId,
            String locationNodeCode,
            String name,
            Boolean enabled,
            String pageCode,
            Pageable pageable
    ) {
        return springDataRepository.findAllWithFilters(
                modelId,
                blankToNull(locationNodeCode),
                blankToNull(name),
                enabled,
                blankToNull(pageCode),
                pageable
        );
    }

    @Override
    public boolean existsByLocationNodeAndName(LocationNode locationNode, String name) {
        return springDataRepository.existsByLocationNodeAndName(locationNode, name);
    }

    @Override
    public boolean existsByLocationNodeAndNameAndIdNot(LocationNode locationNode, String name, Integer id) {
        return springDataRepository.existsByLocationNodeAndNameAndIdNot(locationNode, name, id);
    }

    @Override
    public boolean existsByDeviceModelId(Integer deviceModelId) {
        return springDataRepository.existsByDeviceModel_Id(deviceModelId);
    }

    @Override
    public List<Device> findAllEnabledForCapabilities(Collection<String> locationNodeCodes, String pageCode) {
        return springDataRepository.findAllEnabledForCapabilities(locationNodeCodes, blankToNull(pageCode));
    }

    @Override
    public void delete(Device device) {
        springDataRepository.delete(device);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
