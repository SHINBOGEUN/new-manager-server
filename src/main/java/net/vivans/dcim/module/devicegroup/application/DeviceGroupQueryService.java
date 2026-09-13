package net.vivans.dcim.module.devicegroup.application;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import net.vivans.dcim.module.devicegroup.api.dto.DeviceGroupRequest;
import net.vivans.dcim.module.devicegroup.api.dto.DeviceGroupResponse;
import net.vivans.dcim.module.devicegroup.domain.model.DeviceGroup;
import net.vivans.dcim.module.devicegroup.domain.repository.DeviceGroupRepository;
import net.vivans.dcim.module.pue.application.PueCollectorSyncService;
import net.vivans.dcim.module.pue.domain.repository.PueDefinitionRepository;
import net.vivans.dcim.shared.exception.ConflictException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceGroupQueryService {

    private final DeviceGroupRepository deviceGroupRepository;
    private final DeviceRepository deviceRepository;
    private final PueDefinitionRepository pueDefinitionRepository;
    private final PueCollectorSyncService pueCollectorSyncService;

    public List<DeviceGroupResponse> getDeviceGroups(Boolean enabled) {
        return deviceGroupRepository.findAll(enabled).stream().map(DeviceGroupResponse::from).toList();
    }

    public DeviceGroupResponse getDeviceGroup(Integer id) {
        return DeviceGroupResponse.from(findDeviceGroup(id));
    }

    @Transactional
    public DeviceGroupResponse createDeviceGroup(DeviceGroupRequest request) {
        String name = normalizedName(request.name());
        if (deviceGroupRepository.existsByName(name)) {
            throw new ConflictException("device group name already exists: " + name);
        }
        DeviceGroup group = DeviceGroup.create(name, request.description(), request.enabled() == null || request.enabled());
        group.replaceDevices(findDevices(request.deviceIds()));
        return DeviceGroupResponse.from(deviceGroupRepository.save(group));
    }

    @Transactional
    public DeviceGroupResponse updateDeviceGroup(Integer id, DeviceGroupRequest request) {
        DeviceGroup group = findDeviceGroup(id);
        String name = normalizedName(request.name());
        if (deviceGroupRepository.existsByNameAndIdNot(name, id)) {
            throw new ConflictException("device group name already exists: " + name);
        }
        group.update(name, request.description(), request.enabled() == null || request.enabled(), findDevices(request.deviceIds()));
        DeviceGroup saved = deviceGroupRepository.save(group);
        pueDefinitionRepository.findAllByDeviceGroupId(id).forEach(definition -> {
            definition.refreshResolvedSources();
            pueCollectorSyncService.sync(pueDefinitionRepository.save(definition));
        });
        return DeviceGroupResponse.from(saved);
    }

    @Transactional
    public Integer deleteDeviceGroup(Integer id) {
        DeviceGroup group = findDeviceGroup(id);
        if (pueDefinitionRepository.existsByDeviceGroupId(id)) {
            throw new ConflictException("PUE 수집 정의에서 사용하는 장비 그룹은 삭제할 수 없습니다. PUE 정의에서 먼저 변경하세요.");
        }
        deviceGroupRepository.delete(group);
        return id;
    }

    private DeviceGroup findDeviceGroup(Integer id) {
        return deviceGroupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("DeviceGroup not found: " + id));
    }

    private Collection<Device> findDevices(List<Integer> deviceIds) {
        if (deviceIds == null || deviceIds.isEmpty()) {
            return List.of();
        }
        Set<Integer> uniqueIds = new LinkedHashSet<>(deviceIds);
        return uniqueIds.stream().map(id -> deviceRepository.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("Device not found: " + id)))
                .toList();
    }

    private static String normalizedName(String value) {
        return value == null ? null : value.trim();
    }
}
