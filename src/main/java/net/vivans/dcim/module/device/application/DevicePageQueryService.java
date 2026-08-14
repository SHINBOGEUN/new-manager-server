package net.vivans.dcim.module.device.application;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.common.domain.repository.CommonCodeRepository;
import net.vivans.dcim.module.device.api.dto.DevicePageCreateRequest;
import net.vivans.dcim.module.device.api.dto.DevicePageReplaceRequest;
import net.vivans.dcim.module.device.api.dto.DevicePageResponse;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.model.DevicePage;
import net.vivans.dcim.module.device.domain.repository.DevicePageRepository;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import net.vivans.dcim.shared.exception.ConflictException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DevicePageQueryService {

    private static final String ALREADY_LINKED_MESSAGE = "page already linked to this device";

    private final DeviceRepository deviceRepository;
    private final DevicePageRepository devicePageRepository;
    private final CommonCodeRepository commonCodeRepository;

    public List<DevicePageResponse> getDevicePages(Integer deviceId) {
        findDevice(deviceId);
        List<DevicePage> pages = devicePageRepository.findAllByDeviceIdOrderByIdAsc(deviceId);
        List<DevicePageResponse> responses = new ArrayList<>();
        for (DevicePage page : pages) {
            responses.add(DevicePageResponse.from(page));
        }
        return responses;
    }

    @Transactional
    public DevicePageResponse createDevicePage(Integer deviceId, DevicePageCreateRequest request) {
        Device device = findDevice(deviceId);
        CommonCode pageCode = findPageCode(request.pageCodeId());

        if (devicePageRepository.existsByDeviceIdAndPageCodeId(deviceId, pageCode.getId())) {
            throw new ConflictException(ALREADY_LINKED_MESSAGE);
        }

        DevicePage devicePage = DevicePage.create(device, pageCode);
        return DevicePageResponse.from(devicePageRepository.save(devicePage));
    }

    @Transactional
    public List<DevicePageResponse> replaceDevicePages(Integer deviceId, DevicePageReplaceRequest request) {
        Device device = findDevice(deviceId);
        List<DevicePage> existing = devicePageRepository.findAllByDeviceIdOrderByIdAsc(deviceId);
        devicePageRepository.deleteAll(existing);

        Set<Integer> uniqueIds = new LinkedHashSet<>(request.pageCodeIds());
        List<DevicePage> created = new ArrayList<>();
        for (Integer pageCodeId : uniqueIds) {
            CommonCode pageCode = findPageCode(pageCodeId);
            created.add(DevicePage.create(device, pageCode));
        }

        List<DevicePage> saved = devicePageRepository.saveAll(created);
        List<DevicePageResponse> responses = new ArrayList<>();
        for (DevicePage page : saved) {
            responses.add(DevicePageResponse.from(page));
        }
        return responses;
    }

    @Transactional
    public Integer deleteDevicePage(Integer deviceId, Integer pageId) {
        findDevice(deviceId);
        DevicePage devicePage = devicePageRepository.findByIdAndDeviceId(pageId, deviceId)
                .orElseThrow(() -> new EntityNotFoundException("DevicePage not found: " + pageId));
        devicePageRepository.delete(devicePage);
        return pageId;
    }

    private Device findDevice(Integer deviceId) {
        return deviceRepository.findById(deviceId)
                .orElseThrow(() -> new EntityNotFoundException("Device not found: " + deviceId));
    }

    private CommonCode findPageCode(Integer pageCodeId) {
        CommonCode pageCode = commonCodeRepository.findById(pageCodeId)
                .orElseThrow(() -> new EntityNotFoundException("CommonCode not found: " + pageCodeId));
        if (!DevicePage.DEVICE_PAGE_GROUP_KEY.equals(pageCode.getCodeGroup().getGroupKey())) {
            throw new IllegalArgumentException("pageCode must belong to DEVICE_PAGE group");
        }
        return pageCode;
    }
}
