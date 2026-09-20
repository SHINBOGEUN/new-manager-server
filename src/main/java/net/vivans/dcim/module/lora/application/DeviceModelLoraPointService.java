package net.vivans.dcim.module.lora.application;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.common.domain.repository.CommonCodeRepository;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModel;
import net.vivans.dcim.module.devicemodel.domain.repository.DeviceModelRepository;
import net.vivans.dcim.module.lora.api.dto.DeviceModelLoraPointRequest;
import net.vivans.dcim.module.lora.api.dto.DeviceModelLoraPointResponse;
import net.vivans.dcim.module.lora.domain.model.DeviceModelLoraPoint;
import net.vivans.dcim.module.lora.domain.repository.DeviceModelLoraPointRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceModelLoraPointService {

    private static final String DATA_POINT_TYPE_GROUP = "DATA_POINT_TYPE";
    private static final String DEFAULT_DATA_POINT_TYPE = "UNCLASSIFIED";

    private final DeviceModelLoraPointRepository deviceModelLoraPointRepository;
    private final DeviceModelRepository deviceModelRepository;
    private final CommonCodeRepository commonCodeRepository;
    private final LoraValueMapValidator loraValueMapValidator;
    private final LoraModelTypeValidator loraModelTypeValidator;

    public List<DeviceModelLoraPointResponse> getAllByModelId(Integer deviceModelId) {
        List<DeviceModelLoraPointResponse> responses = new ArrayList<>();
        for (DeviceModelLoraPoint point : deviceModelLoraPointRepository.findAllByDeviceModelIdOrderByIdAsc(deviceModelId)) {
            responses.add(DeviceModelLoraPointResponse.from(point));
        }
        return responses;
    }

    @Transactional
    public DeviceModelLoraPointResponse create(Integer deviceModelId, DeviceModelLoraPointRequest request) {
        DeviceModel deviceModel = findDeviceModel(deviceModelId);
        loraModelTypeValidator.requireLoraSensor(deviceModel);
        loraValueMapValidator.validate(request.valueMap());
        validateUnique(deviceModelId, request.payloadField(), request.pointName(), null);
        boolean enabled = request.enabled() == null || request.enabled();
        DeviceModelLoraPoint saved = deviceModelLoraPointRepository.save(DeviceModelLoraPoint.create(
                deviceModel, request.payloadField(), request.pointName(),
                resolveDataPointType(request.dataPointTypeId()), request.unit(), request.scale(),
                request.valueMap(), enabled));
        return DeviceModelLoraPointResponse.from(saved);
    }

    @Transactional
    public DeviceModelLoraPointResponse update(Integer deviceModelId, Integer id, DeviceModelLoraPointRequest request) {
        DeviceModelLoraPoint point = findPoint(deviceModelId, id);
        loraModelTypeValidator.requireLoraSensor(point.getDeviceModel());
        loraValueMapValidator.validate(request.valueMap());
        validateUnique(deviceModelId, request.payloadField(), request.pointName(), id);
        boolean enabled = request.enabled() == null || request.enabled();
        point.update(request.payloadField(), request.pointName(), resolveDataPointType(request.dataPointTypeId()),
                request.unit(), request.scale(), request.valueMap(), enabled);
        return DeviceModelLoraPointResponse.from(deviceModelLoraPointRepository.save(point));
    }

    @Transactional
    public Integer delete(Integer deviceModelId, Integer id) {
        DeviceModelLoraPoint point = findPoint(deviceModelId, id);
        deviceModelLoraPointRepository.delete(point);
        return id;
    }

    private void validateUnique(Integer deviceModelId, String payloadField, String pointName, Integer excludeId) {
        boolean fieldConflict = excludeId == null
                ? deviceModelLoraPointRepository.existsByDeviceModelIdAndPayloadField(deviceModelId, payloadField)
                : deviceModelLoraPointRepository.existsByDeviceModelIdAndPayloadFieldAndIdNot(deviceModelId, payloadField, excludeId);
        if (fieldConflict) {
            throw new IllegalArgumentException("payloadField already mapped for this model");
        }
        boolean nameConflict = excludeId == null
                ? deviceModelLoraPointRepository.existsByDeviceModelIdAndPointName(deviceModelId, pointName)
                : deviceModelLoraPointRepository.existsByDeviceModelIdAndPointNameAndIdNot(deviceModelId, pointName, excludeId);
        if (nameConflict) {
            throw new IllegalArgumentException("pointName already used for this model");
        }
    }

    private DeviceModel findDeviceModel(Integer deviceModelId) {
        return deviceModelRepository.findById(deviceModelId)
                .orElseThrow(() -> new EntityNotFoundException("DeviceModel not found: " + deviceModelId));
    }

    private DeviceModelLoraPoint findPoint(Integer deviceModelId, Integer id) {
        DeviceModelLoraPoint point = deviceModelLoraPointRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("DeviceModelLoraPoint not found: " + id));
        if (!point.getDeviceModel().getId().equals(deviceModelId)) {
            throw new EntityNotFoundException("DeviceModelLoraPoint not found: " + id);
        }
        return point;
    }

    private CommonCode resolveDataPointType(Integer id) {
        if (id == null) {
            return commonCodeRepository.findByCodeGroupGroupKeyAndCode(DATA_POINT_TYPE_GROUP, DEFAULT_DATA_POINT_TYPE)
                    .orElseThrow(() -> new EntityNotFoundException(DATA_POINT_TYPE_GROUP + "/" + DEFAULT_DATA_POINT_TYPE + " is not configured"));
        }
        CommonCode code = commonCodeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("CommonCode not found: " + id));
        if (!DATA_POINT_TYPE_GROUP.equals(code.getCodeGroup().getGroupKey())) {
            throw new IllegalArgumentException("dataPointTypeId must belong to DATA_POINT_TYPE");
        }
        return code;
    }
}
