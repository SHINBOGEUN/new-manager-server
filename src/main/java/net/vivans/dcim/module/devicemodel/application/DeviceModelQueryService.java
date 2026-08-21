package net.vivans.dcim.module.devicemodel.application;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.collectortask.application.CollectionScriptSyncService;
import net.vivans.dcim.module.collectortask.domain.repository.CollectionTaskRepository;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.common.domain.repository.CommonCodeRepository;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import net.vivans.dcim.module.devicemodel.api.dto.DeviceModelCreateRequest;
import net.vivans.dcim.module.devicemodel.api.dto.DeviceModelProtocolRequest;
import net.vivans.dcim.module.devicemodel.api.dto.DeviceModelResponse;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModel;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModelProtocol;
import net.vivans.dcim.module.devicemodel.domain.repository.DeviceModelRepository;
import net.vivans.dcim.shared.exception.ConflictException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceModelQueryService {

    private static final String PROTOCOL_TYPE_GROUP_KEY = "PROTOCOL_TYPE";
    private static final String MODEL_TYPE_GROUP_KEY = "MODEL_TYPE";
    private static final String DEVICE_MODEL_REFERENCED_MESSAGE = "device model is referenced by devices";
    private static final String DEVICE_MODEL_REFERENCED_BY_TASK_MESSAGE =
            "device model is referenced by collection tasks";

    private final DeviceModelRepository deviceModelRepository;
    private final CommonCodeRepository commonCodeRepository;
    private final DeviceRepository deviceRepository;
    private final CollectionTaskRepository collectionTaskRepository;
    private final CollectionScriptSyncService collectionScriptSyncService;

    @Transactional
    public DeviceModelResponse createDeviceModel(DeviceModelCreateRequest request) {
        validateUniqueNameAndManufacturer(request.name(), request.manufacturer(), null);

        CommonCode deviceType = findDeviceType(request.deviceTypeId());
        DeviceModel deviceModel = DeviceModel.create(
                request.name(),
                request.manufacturer(),
                deviceType,
                request.description()
        );
        replaceProtocolsFromRequest(deviceModel, request.protocols());

        return DeviceModelResponse.from(deviceModelRepository.save(deviceModel));
    }

    @Transactional
    public DeviceModelResponse updateDeviceModel(Integer id, DeviceModelCreateRequest request) {
        DeviceModel deviceModel = findDeviceModel(id);
        validateUniqueNameAndManufacturer(request.name(), request.manufacturer(), id);

        CommonCode deviceType = findDeviceType(request.deviceTypeId());
        deviceModel.update(request.name(), request.manufacturer(), deviceType, request.description());
        replaceProtocolsFromRequest(deviceModel, request.protocols());

        DeviceModelResponse response = DeviceModelResponse.from(deviceModelRepository.save(deviceModel));
        collectionScriptSyncService.regenerateByModelId(id);
        return response;
    }

    public List<DeviceModelResponse> getDeviceModels(String name, String manufacturer) {
        List<DeviceModel> deviceModels = deviceModelRepository.findAll(name, manufacturer);
        List<DeviceModelResponse> responses = new ArrayList<>();
        for (DeviceModel deviceModel : deviceModels) {
            responses.add(DeviceModelResponse.from(deviceModel));
        }
        return responses;
    }

    public DeviceModelResponse getDeviceModel(Integer id) {
        return DeviceModelResponse.from(findDeviceModel(id));
    }

    @Transactional
    public void deleteDeviceModel(Integer id) {
        DeviceModel deviceModel = findDeviceModel(id);
        if (deviceRepository.existsByDeviceModelId(id)) {
            throw new ConflictException(DEVICE_MODEL_REFERENCED_MESSAGE);
        }
        if (collectionTaskRepository.existsByModelId(id)) {
            throw new ConflictException(DEVICE_MODEL_REFERENCED_BY_TASK_MESSAGE);
        }
        deviceModelRepository.delete(deviceModel);
    }

    private DeviceModel findDeviceModel(Integer id) {
        return deviceModelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("DeviceModel not found: " + id));
    }

    private void validateUniqueNameAndManufacturer(String name, String manufacturer, Integer excludeId) {
        boolean exists = excludeId == null
                ? deviceModelRepository.existsByNameAndManufacturer(name, manufacturer)
                : deviceModelRepository.existsByNameAndManufacturerAndIdNot(name, manufacturer, excludeId);
        if (exists) {
            throw new IllegalArgumentException("device model already exists");
        }
    }

    /**
     * API 요청의 protocols[]를 검증한 뒤, DeviceModel에 연결된 프로토콜 목록을 통째로 교체합니다.
     * 등록·수정 모두 동일하게 사용합니다 (수정 시 기존 device_model_protocol 행은 전부 삭제 후 재등록).
     */
    private void replaceProtocolsFromRequest(DeviceModel deviceModel, List<DeviceModelProtocolRequest> protocolRequests) {
        validateProtocolRequests(protocolRequests);

        List<DeviceModelProtocol> protocols = new ArrayList<>();
        for (DeviceModelProtocolRequest request : protocolRequests) {
            CommonCode protocolType = findProtocolType(request.protocolTypeId());
            protocols.add(DeviceModelProtocol.of(deviceModel, protocolType));
        }

        // 수정 시 동일 protocol_type_id가 남아 있으면 INSERT가 DELETE보다 먼저 실행되어 UK 위반 가능
        if (deviceModel.getId() != null && !deviceModel.getProtocols().isEmpty()) {
            deviceModel.replaceProtocols(List.of());
            deviceModelRepository.flush();
        }
        deviceModel.replaceProtocols(protocols);
    }

    private void validateProtocolRequests(List<DeviceModelProtocolRequest> protocolRequests) {
        if (protocolRequests == null || protocolRequests.isEmpty()) {
            throw new IllegalArgumentException("at least one protocol required");
        }

        Set<Integer> protocolTypeIds = new HashSet<>();
        for (DeviceModelProtocolRequest request : protocolRequests) {
            if (!protocolTypeIds.add(request.protocolTypeId())) {
                throw new IllegalArgumentException("duplicate protocol type in request");
            }
        }
    }

    private CommonCode findProtocolType(Integer protocolTypeId) {
        CommonCode protocolType = commonCodeRepository.findById(protocolTypeId)
                .orElseThrow(() -> new EntityNotFoundException("CommonCode not found: " + protocolTypeId));

        if (!PROTOCOL_TYPE_GROUP_KEY.equals(protocolType.getCodeGroup().getGroupKey())) {
            throw new IllegalArgumentException("protocolType must belong to PROTOCOL_TYPE group");
        }
        return protocolType;
    }

    private CommonCode findDeviceType(Integer deviceTypeId) {
        CommonCode deviceType = commonCodeRepository.findById(deviceTypeId)
                .orElseThrow(() -> new EntityNotFoundException("CommonCode not found: " + deviceTypeId));

        if (!MODEL_TYPE_GROUP_KEY.equals(deviceType.getCodeGroup().getGroupKey())) {
            throw new IllegalArgumentException("deviceType must belong to MODEL_TYPE group");
        }
        return deviceType;
    }
}
