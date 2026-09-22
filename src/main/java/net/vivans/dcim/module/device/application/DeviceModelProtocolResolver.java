package net.vivans.dcim.module.device.application;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModel;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModelProtocol;
import net.vivans.dcim.module.devicemodel.domain.repository.DeviceModelRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DeviceModelProtocolResolver {

    private final DeviceModelRepository deviceModelRepository;

    public DeviceModel resolveModel(Device device) {
        Integer modelId = device.getDeviceModel().getId();
        return deviceModelRepository.findById(modelId)
                .orElseThrow(() -> new EntityNotFoundException("DeviceModel not found: " + modelId));
    }

    public Optional<DeviceModelProtocol> findByCode(DeviceModel deviceModel, String protocolCode) {
        return deviceModel.getProtocols().stream()
                .filter(protocol -> protocolCode.equals(protocol.getProtocolType().getCode()))
                .findFirst();
    }

    public boolean supports(DeviceModel deviceModel, Integer protocolTypeId) {
        return deviceModel.getProtocols().stream()
                .anyMatch(protocol -> protocolTypeId.equals(protocol.getProtocolType().getId()));
    }
}
