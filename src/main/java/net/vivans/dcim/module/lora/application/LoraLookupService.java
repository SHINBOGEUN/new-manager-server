package net.vivans.dcim.module.lora.application;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.lora.api.dto.DeviceLoraEndpointResponse;
import net.vivans.dcim.module.lora.api.dto.DeviceModelLoraPointResponse;
import net.vivans.dcim.module.lora.api.dto.LoraDeviceLookupResponse;
import net.vivans.dcim.module.mqtt.domain.model.DeviceMqttIdentity;
import net.vivans.dcim.module.mqtt.domain.model.MqttExternalIdNormalizer;
import net.vivans.dcim.module.mqtt.domain.model.MqttIdentityType;
import net.vivans.dcim.module.mqtt.domain.repository.DeviceMqttIdentityRepository;
import net.vivans.dcim.module.mqtt.domain.repository.DeviceModelMqttPointRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Sensor Data 서버가 TTL 캐시를 채우기 위해 호출하는 조회 전용 API의 서비스 계층.
 * 대량 pull(전체 endpoint / 전체 활성 매핑)과, 캐시 미스 시 즉시 조회용 단건 resolve를 함께 제공한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoraLookupService {

    private final DeviceMqttIdentityRepository mqttIdentityRepository;
    private final DeviceModelMqttPointRepository mqttPointRepository;

    public Optional<LoraDeviceLookupResponse> resolve(MqttIdentityType idType, String externalId) {
        if (idType == null || !idType.isLora()) {
            throw new IllegalArgumentException("LoRa idType must be DEV_EUI or DEVICE_NAME");
        }
        String normalized = MqttExternalIdNormalizer.normalize(idType, externalId);
        return mqttIdentityRepository
                .findByIdentityTypeAndNormalizedExternalIdAndEnabledTrue(idType, normalized)
                .map(endpoint -> new LoraDeviceLookupResponse(
                        endpoint.getDevice().getId(),
                        endpoint.getDevice().getName(),
                        endpoint.getDevice().getDeviceModel().getId(),
                        endpoint.getDevice().getDeviceModel().getName(),
                        endpoint.isEnabled()
                ));
    }

    public List<DeviceLoraEndpointResponse> getAllEnabledEndpoints() {
        List<DeviceLoraEndpointResponse> responses = new ArrayList<>();
        for (DeviceMqttIdentity endpoint : mqttIdentityRepository.findAllOrderByIdAsc()) {
            if (endpoint.isEnabled() && LoraMqttSelection.isLora(endpoint)) {
                responses.add(DeviceLoraEndpointResponse.from(endpoint));
            }
        }
        return responses;
    }

    public List<DeviceModelLoraPointResponse> getAllEnabledModelMappings() {
        List<DeviceModelLoraPointResponse> responses = new ArrayList<>();
        mqttPointRepository.findAllEnabled().stream()
                .filter(LoraMqttSelection::isLora)
                .forEach(point -> responses.add(DeviceModelLoraPointResponse.from(point)));
        return responses;
    }

}
