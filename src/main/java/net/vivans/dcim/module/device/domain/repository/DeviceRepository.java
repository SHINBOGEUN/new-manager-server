package net.vivans.dcim.module.device.domain.repository;

import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.location.domain.model.LocationNode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DeviceRepository {

    Device save(Device device);

    List<Device> saveAll(Iterable<Device> devices);

    Optional<Device> findById(Integer id);

    List<Device> findByLocationNodeCode(String locationNodeCode);

    List<Device> findByLocationNodeCodeIn(Collection<String> locationNodeCodes);

    Page<Device> findAll(
            Integer modelId,
            String locationNodeCode,
            String name,
            Boolean enabled,
            String pageCode,
            Pageable pageable
    );

    List<Device> findAllEnabledForCapabilities(Collection<String> locationNodeCodes, String pageCode);

    boolean existsByLocationNodeAndName(LocationNode locationNode, String name);

    boolean existsByLocationNodeAndNameAndIdNot(LocationNode locationNode, String name, Integer id);

    boolean existsByDeviceModelId(Integer deviceModelId);

    void delete(Device device);
}
