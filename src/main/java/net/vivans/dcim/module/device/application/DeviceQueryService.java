package net.vivans.dcim.module.device.application;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.api.dto.DeviceCreateRequest;
import net.vivans.dcim.module.device.api.dto.DeviceResponse;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.model.DeviceProtocolEndpoint;
import net.vivans.dcim.module.device.domain.repository.DeviceProtocolEndpointRepository;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModel;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModelProtocol;
import net.vivans.dcim.module.devicemodel.domain.repository.DeviceModelRepository;
import net.vivans.dcim.module.location.domain.model.LocationNode;
import net.vivans.dcim.module.location.domain.repository.LocationNodeRepository;
import net.vivans.dcim.shared.api.PageResponse;
import net.vivans.dcim.shared.exception.ConflictException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceQueryService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final String ENDPOINTS_NOT_SUPPORTED_BY_NEW_MODEL_MESSAGE =
            "device has endpoints not supported by new model";

    private final DeviceRepository deviceRepository;
    private final DeviceModelRepository deviceModelRepository;
    private final LocationNodeRepository locationNodeRepository;
    private final DeviceProtocolEndpointRepository deviceProtocolEndpointRepository;

    public PageResponse<DeviceResponse> getDevices(
            Integer modelId,
            String locationNodeCode,
            String name,
            Boolean enabled,
            String pageCode,
            int page,
            int size
    ) {
        int safePage = Math.max(page, 1) - 1;
        int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, "id"));

        return PageResponse.from(
                deviceRepository.findAll(modelId, locationNodeCode, name, enabled, pageCode, pageable),
                DeviceResponse::from
        );
    }

    public DeviceResponse getDevice(Integer id) {
        return DeviceResponse.from(findDevice(id));
    }

    @Transactional
    public DeviceResponse createDevice(DeviceCreateRequest request) {
        DeviceModel deviceModel = findDeviceModel(request.modelId());
        LocationNode locationNode = findLocationNode(request.locationNodeCode());
        validateUniqueNameAtLocation(locationNode, request.name(), null);

        boolean enabled = request.enabled() == null || request.enabled();
        Device device = Device.create(
                deviceModel,
                locationNode,
                request.name(),
                request.description(),
                enabled
        );
        return DeviceResponse.from(deviceRepository.save(device));
    }

    @Transactional
    public DeviceResponse updateDevice(Integer id, DeviceCreateRequest request) {
        Device device = findDevice(id);
        DeviceModel deviceModel = findDeviceModel(request.modelId());
        LocationNode locationNode = findLocationNode(request.locationNodeCode());
        validateUniqueNameAtLocation(locationNode, request.name(), id);
        if (!device.getDeviceModel().getId().equals(deviceModel.getId())) {
            validateEndpointsCompatibleWithModel(id, deviceModel);
        }

        boolean enabled = request.enabled() == null || request.enabled();
        device.update(
                deviceModel,
                locationNode,
                request.name(),
                request.description(),
                enabled
        );
        return DeviceResponse.from(deviceRepository.save(device));
    }

    @Transactional
    public void deleteDevice(Integer id) {
        Device device = findDevice(id);
        deviceRepository.delete(device);
    }

    private Device findDevice(Integer id) {
        return deviceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Device not found: " + id));
    }

    private DeviceModel findDeviceModel(Integer modelId) {
        return deviceModelRepository.findById(modelId)
                .orElseThrow(() -> new EntityNotFoundException("DeviceModel not found: " + modelId));
    }

    private LocationNode findLocationNode(String locationNodeCode) {
        return locationNodeRepository.findByCode(locationNodeCode)
                .orElseThrow(() -> new EntityNotFoundException("LocationNode not found: " + locationNodeCode));
    }

    private void validateUniqueNameAtLocation(LocationNode locationNode, String name, Integer excludeId) {
        boolean duplicated = excludeId == null
                ? deviceRepository.existsByLocationNodeAndName(locationNode, name)
                : deviceRepository.existsByLocationNodeAndNameAndIdNot(locationNode, name, excludeId);
        if (duplicated) {
            throw new IllegalArgumentException("device name already exists at this location");
        }
    }

    private void validateEndpointsCompatibleWithModel(Integer deviceId, DeviceModel newModel) {
        List<DeviceProtocolEndpoint> endpoints =
                deviceProtocolEndpointRepository.findAllByDeviceIdOrderByIdAsc(deviceId);
        if (endpoints.isEmpty()) {
            return;
        }

        Set<Integer> supportedProtocolTypeIds = new HashSet<>();
        for (DeviceModelProtocol protocol : newModel.getProtocols()) {
            supportedProtocolTypeIds.add(protocol.getProtocolType().getId());
        }

        List<String> unsupportedProtocolCodes = new ArrayList<>();
        for (DeviceProtocolEndpoint endpoint : endpoints) {
            if (!supportedProtocolTypeIds.contains(endpoint.getProtocolType().getId())) {
                unsupportedProtocolCodes.add(endpoint.getProtocolType().getCode());
            }
        }

        if (!unsupportedProtocolCodes.isEmpty()) {
            throw new ConflictException(
                    ENDPOINTS_NOT_SUPPORTED_BY_NEW_MODEL_MESSAGE + ": "
                            + String.join(", ", unsupportedProtocolCodes));
        }
    }
}
