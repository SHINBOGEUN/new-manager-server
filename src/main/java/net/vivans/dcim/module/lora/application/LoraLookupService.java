package net.vivans.dcim.module.lora.application;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.lora.api.dto.DeviceLoraEndpointResponse;
import net.vivans.dcim.module.lora.api.dto.DeviceModelLoraPointResponse;
import net.vivans.dcim.module.lora.api.dto.DeviceLoraPointOverrideResponse;
import net.vivans.dcim.module.lora.api.dto.LoraDeviceLookupResponse;
import net.vivans.dcim.module.lora.domain.model.DeviceLoraEndpoint;
import net.vivans.dcim.module.lora.domain.model.LoraExternalIdNormalizer;
import net.vivans.dcim.module.lora.domain.model.LoraIdType;
import net.vivans.dcim.module.lora.domain.repository.DeviceLoraEndpointRepository;
import net.vivans.dcim.module.lora.domain.repository.DeviceLoraPointOverrideRepository;
import net.vivans.dcim.module.lora.domain.repository.DeviceModelLoraPointRepository;
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

    private final DeviceLoraEndpointRepository deviceLoraEndpointRepository;
    private final DeviceModelLoraPointRepository deviceModelLoraPointRepository;
    private final DeviceLoraPointOverrideRepository deviceLoraPointOverrideRepository;

    public Optional<LoraDeviceLookupResponse> resolve(LoraIdType idType, String externalId) {
        String normalized = LoraExternalIdNormalizer.normalize(externalId);
        return deviceLoraEndpointRepository
                .findByIdTypeAndNormalizedExternalIdAndEnabledTrue(idType, normalized)
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
        for (DeviceLoraEndpoint endpoint : deviceLoraEndpointRepository.findAllOrderByIdAsc()) {
            if (endpoint.isEnabled()) {
                responses.add(DeviceLoraEndpointResponse.from(endpoint));
            }
        }
        return responses;
    }

    public List<DeviceModelLoraPointResponse> getAllEnabledModelMappings() {
        List<DeviceModelLoraPointResponse> responses = new ArrayList<>();
        deviceModelLoraPointRepository.findAllEnabled().forEach(point -> responses.add(DeviceModelLoraPointResponse.from(point)));
        return responses;
    }

    public List<DeviceLoraPointOverrideResponse> getAllEnabledOverrides() {
        List<DeviceLoraPointOverrideResponse> responses = new ArrayList<>();
        deviceLoraPointOverrideRepository.findAllEnabled().forEach(override -> responses.add(DeviceLoraPointOverrideResponse.from(override)));
        return responses;
    }
}
