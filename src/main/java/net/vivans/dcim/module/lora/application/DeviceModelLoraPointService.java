package net.vivans.dcim.module.lora.application;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModel;
import net.vivans.dcim.module.devicemodel.domain.repository.DeviceModelRepository;
import net.vivans.dcim.module.common.application.UnitCodeResolver;
import net.vivans.dcim.module.common.application.CategoryCodeResolver;
import net.vivans.dcim.module.lora.api.dto.DeviceModelLoraPointRequest;
import net.vivans.dcim.module.lora.api.dto.DeviceModelLoraPointResponse;
import net.vivans.dcim.module.mqtt.domain.model.DeviceModelMqttPoint;
import net.vivans.dcim.module.mqtt.domain.model.MqttPointScope;
import net.vivans.dcim.module.mqtt.domain.repository.DeviceModelMqttPointRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceModelLoraPointService {

    private final DeviceModelMqttPointRepository mqttPointRepository;
    private final DeviceModelRepository deviceModelRepository;
    private final LoraDataPointTypeResolver dataPointTypeResolver;
    private final LoraValueMapValidator loraValueMapValidator;
    private final LoraModelTypeValidator loraModelTypeValidator;
    private final UnitCodeResolver unitCodeResolver;
    private final CategoryCodeResolver categoryCodeResolver;
    private final ApplicationEventPublisher eventPublisher;

    public List<DeviceModelLoraPointResponse> getAllByModelId(Integer deviceModelId) {
        List<DeviceModelLoraPointResponse> responses = new ArrayList<>();
        for (DeviceModelMqttPoint point : mqttPointRepository.findAllByDeviceModelIdOrderByIdAsc(deviceModelId)) {
            if (LoraMqttSelection.isLora(point)) {
                responses.add(DeviceModelLoraPointResponse.from(point));
            }
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
        DeviceModelMqttPoint saved = mqttPointRepository.save(DeviceModelMqttPoint.create(
                deviceModel, request.payloadField(), request.pointName(),
                dataPointTypeResolver.resolve(request.dataPointTypeId()), unitCodeResolver.resolve(request.unitCodeId()), request.scale(),
                request.valueMap(), enabled, categoryCodeResolver.resolve(request.categoryCodeId())));
        eventPublisher.publishEvent(new LoraConfigChangedEvent("model-mapping-created:" + saved.getId()));
        return DeviceModelLoraPointResponse.from(saved);
    }

    @Transactional
    public DeviceModelLoraPointResponse update(Integer deviceModelId, Integer id, DeviceModelLoraPointRequest request) {
        DeviceModelMqttPoint point = findPoint(deviceModelId, id);
        loraModelTypeValidator.requireLoraSensor(point.getDeviceModel());
        loraValueMapValidator.validate(request.valueMap());
        validateUnique(deviceModelId, request.payloadField(), request.pointName(), id);
        boolean enabled = request.enabled() == null || request.enabled();
        point.update(request.payloadField(), request.pointName(), dataPointTypeResolver.resolve(request.dataPointTypeId()),
                unitCodeResolver.resolve(request.unitCodeId()), request.scale(), request.valueMap(), enabled,
                categoryCodeResolver.resolve(request.categoryCodeId()));
        DeviceModelMqttPoint saved = mqttPointRepository.save(point);
        eventPublisher.publishEvent(new LoraConfigChangedEvent("model-mapping-updated:" + id));
        return DeviceModelLoraPointResponse.from(saved);
    }

    @Transactional
    public Integer delete(Integer deviceModelId, Integer id) {
        DeviceModelMqttPoint point = findPoint(deviceModelId, id);
        loraModelTypeValidator.requireLoraSensor(point.getDeviceModel());
        mqttPointRepository.delete(point);
        eventPublisher.publishEvent(new LoraConfigChangedEvent("model-mapping-deleted:" + id));
        return id;
    }

    private void validateUnique(Integer deviceModelId, String payloadField, String pointName, Integer excludeId) {
        boolean fieldConflict = excludeId == null
                ? mqttPointRepository.existsByDeviceModelIdAndPayloadField(deviceModelId, payloadField)
                : mqttPointRepository.existsByDeviceModelIdAndPayloadFieldAndIdNot(deviceModelId, payloadField, excludeId);
        if (fieldConflict) {
            throw new IllegalArgumentException("payloadField already mapped for this model");
        }
        boolean nameConflict = excludeId == null
                ? mqttPointRepository.existsByDeviceModelIdAndPointName(deviceModelId, pointName)
                : mqttPointRepository.existsByDeviceModelIdAndPointNameAndIdNot(deviceModelId, pointName, excludeId);
        if (nameConflict) {
            throw new IllegalArgumentException("pointName already used for this model");
        }
    }

    private DeviceModel findDeviceModel(Integer deviceModelId) {
        return deviceModelRepository.findById(deviceModelId)
                .orElseThrow(() -> new EntityNotFoundException("DeviceModel not found: " + deviceModelId));
    }

    private DeviceModelMqttPoint findPoint(Integer deviceModelId, Integer id) {
        DeviceModelMqttPoint point = mqttPointRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("DeviceModelMqttPoint not found: " + id));
        if (point.getComponentScope() != MqttPointScope.DEVICE) {
            throw new EntityNotFoundException("LoRa point not found: " + id);
        }
        if (!point.getDeviceModel().getId().equals(deviceModelId)) {
            throw new EntityNotFoundException("DeviceModelMqttPoint not found: " + id);
        }
        return point;
    }

}
