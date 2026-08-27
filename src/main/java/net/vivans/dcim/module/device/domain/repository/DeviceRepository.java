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
            Collection<String> locationNodeCodes,
            String name,
            Boolean enabled,
            String pageCode,
            Pageable pageable
    );

    List<Device> findAllEnabledForCapabilities(Collection<String> locationNodeCodes, String pageCode);

    boolean existsByLocationNodeAndName(LocationNode locationNode, String name);

    boolean existsByLocationNodeAndNameAndIdNot(LocationNode locationNode, String name, Integer id);

    boolean existsByDeviceModelId(Integer deviceModelId);

    List<Device> findAllByDeviceModelId(Integer deviceModelId);

    /** count 위젯 등: enabled=true 인 장비 전체 (deviceModel fetch) */
    List<Device> findAllEnabled();

    /** chart 위젯 models 범위: 지정 모델의 enabled 장비 */
    List<Device> findAllEnabledByDeviceModelIds(Collection<Integer> modelIds);

    void flush();

    void delete(Device device);
}
