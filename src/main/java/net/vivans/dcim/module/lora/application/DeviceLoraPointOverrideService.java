package net.vivans.dcim.module.lora.application;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import net.vivans.dcim.module.lora.api.dto.DeviceLoraPointOverrideRequest;
import net.vivans.dcim.module.lora.api.dto.DeviceLoraPointOverrideResponse;
import net.vivans.dcim.module.lora.domain.model.DeviceLoraPointOverride;
import net.vivans.dcim.module.lora.domain.repository.DeviceLoraPointOverrideRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceLoraPointOverrideService {

    private final DeviceLoraPointOverrideRepository deviceLoraPointOverrideRepository;
    private final DeviceRepository deviceRepository;
    private final LoraDataPointTypeResolver dataPointTypeResolver;
    private final LoraValueMapValidator loraValueMapValidator;
    private final LoraModelTypeValidator loraModelTypeValidator;
    private final ApplicationEventPublisher eventPublisher;

    public List<DeviceLoraPointOverrideResponse> getAllByDeviceId(Integer deviceId) {
        List<DeviceLoraPointOverrideResponse> responses = new ArrayList<>();
        for (DeviceLoraPointOverride override : deviceLoraPointOverrideRepository.findAllByDeviceIdOrderByIdAsc(deviceId)) {
            responses.add(DeviceLoraPointOverrideResponse.from(override));
        }
        return responses;
    }

    @Transactional
    public DeviceLoraPointOverrideResponse create(Integer deviceId, DeviceLoraPointOverrideRequest request) {
        Device device = findDevice(deviceId);
        loraModelTypeValidator.requireLoraSensor(device);
        loraValueMapValidator.validate(request.valueMap());
        validateUnique(deviceId, request.payloadField(), request.pointName(), null);
        boolean enabled = request.enabled() == null || request.enabled();
        DeviceLoraPointOverride saved = deviceLoraPointOverrideRepository.save(DeviceLoraPointOverride.create(
                device, request.payloadField(), request.pointName(),
                dataPointTypeResolver.resolve(request.dataPointTypeId()), request.unit(), request.scale(),
                request.valueMap(), enabled));
        eventPublisher.publishEvent(new LoraConfigChangedEvent("override-created:" + saved.getId()));
        return DeviceLoraPointOverrideResponse.from(saved);
    }

    @Transactional
    public DeviceLoraPointOverrideResponse update(Integer deviceId, Integer id, DeviceLoraPointOverrideRequest request) {
        DeviceLoraPointOverride override = findOverride(deviceId, id);
        loraModelTypeValidator.requireLoraSensor(override.getDevice());
        loraValueMapValidator.validate(request.valueMap());
        validateUnique(deviceId, request.payloadField(), request.pointName(), id);
        boolean enabled = request.enabled() == null || request.enabled();
        override.update(request.payloadField(), request.pointName(), dataPointTypeResolver.resolve(request.dataPointTypeId()),
                request.unit(), request.scale(), request.valueMap(), enabled);
        DeviceLoraPointOverride saved = deviceLoraPointOverrideRepository.save(override);
        eventPublisher.publishEvent(new LoraConfigChangedEvent("override-updated:" + id));
        return DeviceLoraPointOverrideResponse.from(saved);
    }

    @Transactional
    public Integer delete(Integer deviceId, Integer id) {
        DeviceLoraPointOverride override = findOverride(deviceId, id);
        deviceLoraPointOverrideRepository.delete(override);
        eventPublisher.publishEvent(new LoraConfigChangedEvent("override-deleted:" + id));
        return id;
    }

    private void validateUnique(Integer deviceId, String payloadField, String pointName, Integer excludeId) {
        boolean fieldConflict = excludeId == null
                ? deviceLoraPointOverrideRepository.existsByDeviceIdAndPayloadField(deviceId, payloadField)
                : deviceLoraPointOverrideRepository.existsByDeviceIdAndPayloadFieldAndIdNot(deviceId, payloadField, excludeId);
        if (fieldConflict) {
            throw new IllegalArgumentException("payloadField already overridden for this device");
        }
        boolean nameConflict = excludeId == null
                ? deviceLoraPointOverrideRepository.existsByDeviceIdAndPointName(deviceId, pointName)
                : deviceLoraPointOverrideRepository.existsByDeviceIdAndPointNameAndIdNot(deviceId, pointName, excludeId);
        if (nameConflict) {
            throw new IllegalArgumentException("pointName already used for this device");
        }
    }

    private Device findDevice(Integer deviceId) {
        return deviceRepository.findById(deviceId)
                .orElseThrow(() -> new EntityNotFoundException("Device not found: " + deviceId));
    }

    private DeviceLoraPointOverride findOverride(Integer deviceId, Integer id) {
        DeviceLoraPointOverride override = deviceLoraPointOverrideRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("DeviceLoraPointOverride not found: " + id));
        if (!override.getDevice().getId().equals(deviceId)) {
            throw new EntityNotFoundException("DeviceLoraPointOverride not found: " + id);
        }
        return override;
    }

}
