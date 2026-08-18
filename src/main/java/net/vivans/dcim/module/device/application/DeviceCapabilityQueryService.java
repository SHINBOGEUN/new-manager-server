package net.vivans.dcim.module.device.application;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.api.dto.DeviceCapabilityEndpointResponse;
import net.vivans.dcim.module.device.api.dto.DeviceCapabilityPointResponse;
import net.vivans.dcim.module.device.api.dto.DeviceCapabilityResponse;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.model.DeviceProtocolEndpoint;
import net.vivans.dcim.module.device.domain.model.DeviceSnmpInstance;
import net.vivans.dcim.module.device.domain.repository.DeviceProtocolEndpointRepository;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import net.vivans.dcim.module.device.domain.repository.DeviceSnmpInstanceRepository;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModel;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModelProtocol;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModelSnmpPoint;
import net.vivans.dcim.module.devicemodel.domain.repository.DeviceModelRepository;
import net.vivans.dcim.module.devicemodel.domain.repository.DeviceModelSnmpPointRepository;
import net.vivans.dcim.module.location.domain.model.LocationNode;
import net.vivans.dcim.module.location.domain.repository.LocationNodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceCapabilityQueryService {

    private static final String SNMP_PROTOCOL_CODE = "snmp";

    private final DeviceRepository deviceRepository;
    private final LocationNodeRepository locationNodeRepository;
    private final DeviceModelRepository deviceModelRepository;
    private final DeviceModelSnmpPointRepository deviceModelSnmpPointRepository;
    private final DeviceProtocolEndpointRepository deviceProtocolEndpointRepository;
    private final DeviceSnmpInstanceRepository deviceSnmpInstanceRepository;

    public List<DeviceCapabilityResponse> getCapabilities(
            String pageCode,
            String locationNodeCode,
            Boolean includeSubtree
    ) {
        Collection<String> locationNodeCodes = resolveLocationNodeCodes(locationNodeCode, includeSubtree);
        if (locationNodeCodes != null && locationNodeCodes.isEmpty()) {
            return List.of();
        }

        List<Device> devices = deviceRepository.findAllEnabledForCapabilities(
                locationNodeCodes,
                blankToNull(pageCode)
        );

        List<DeviceCapabilityResponse> responses = new ArrayList<>();
        for (Device device : devices) {
            responses.add(buildCapability(device));
        }
        return responses;
    }

    private DeviceCapabilityResponse buildCapability(Device device) {
        DeviceModel deviceModel = deviceModelRepository.findById(device.getDeviceModel().getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "DeviceModel not found: " + device.getDeviceModel().getId()));

        DeviceModelProtocol snmpProtocol = findSnmpProtocol(deviceModel);
        DeviceProtocolEndpoint snmpEndpoint = findSnmpEndpoint(device.getId());
        Integer instanceId = resolveInstanceId(snmpEndpoint);

        List<DeviceCapabilityPointResponse> points = new ArrayList<>();
        if (snmpProtocol != null) {
            for (DeviceModelSnmpPoint point : deviceModelSnmpPointRepository
                    .findAllByModelProtocolIdOrderByIdAsc(snmpProtocol.getId())) {
                if (!point.isEnabled()) {
                    continue;
                }
                points.add(new DeviceCapabilityPointResponse(
                        point.getId(),
                        point.getName(),
                        point.getUnit(),
                        point.getOid(),
                        point.resolveOid(instanceId),
                        point.isRequiresInstance()
                ));
            }
        }

        DeviceCapabilityEndpointResponse endpointResponse = null;
        if (snmpEndpoint != null) {
            endpointResponse = new DeviceCapabilityEndpointResponse(
                    snmpEndpoint.getId(),
                    snmpEndpoint.getHost(),
                    snmpEndpoint.getPort(),
                    instanceId
            );
        }

        return new DeviceCapabilityResponse(
                device.getId(),
                device.getName(),
                device.getLocationNode().getName(),
                deviceModel.getId(),
                deviceModel.getName(),
                deviceModel.getManufacturer(),
                endpointResponse,
                points
        );
    }

    private DeviceModelProtocol findSnmpProtocol(DeviceModel deviceModel) {
        for (DeviceModelProtocol protocol : deviceModel.getProtocols()) {
            if (SNMP_PROTOCOL_CODE.equals(protocol.getProtocolType().getCode())) {
                return protocol;
            }
        }
        return null;
    }

    private DeviceProtocolEndpoint findSnmpEndpoint(Integer deviceId) {
        for (DeviceProtocolEndpoint endpoint : deviceProtocolEndpointRepository
                .findAllByDeviceIdOrderByIdAsc(deviceId)) {
            if (!endpoint.isEnabled()) {
                continue;
            }
            if (SNMP_PROTOCOL_CODE.equals(endpoint.getProtocolType().getCode())) {
                return endpoint;
            }
        }
        return null;
    }

    private Integer resolveInstanceId(DeviceProtocolEndpoint snmpEndpoint) {
        if (snmpEndpoint == null) {
            return null;
        }
        return deviceSnmpInstanceRepository.findByEndpointId(snmpEndpoint.getId())
                .map(DeviceSnmpInstance::getInstanceId)
                .orElse(null);
    }

    private Collection<String> resolveLocationNodeCodes(String locationNodeCode, Boolean includeSubtree) {
        String normalizedLocationNodeCode = blankToNull(locationNodeCode);
        if (normalizedLocationNodeCode == null) {
            return null;
        }

        LocationNode rootNode = locationNodeRepository.findByCode(normalizedLocationNodeCode)
                .orElseThrow(() -> new EntityNotFoundException(
                        "LocationNode not found: " + normalizedLocationNodeCode));

        if (!Boolean.TRUE.equals(includeSubtree)) {
            return List.of(rootNode.getCode());
        }

        List<LocationNode> allNodes = locationNodeRepository.findAll();
        Map<String, List<LocationNode>> childrenByParentCode = buildChildrenByParentCode(allNodes);

        Set<String> subtreeCodes = new HashSet<>();
        subtreeCodes.add(rootNode.getCode());
        collectDescendants(rootNode.getCode(), childrenByParentCode, subtreeCodes);
        return subtreeCodes;
    }

    private Map<String, List<LocationNode>> buildChildrenByParentCode(List<LocationNode> nodes) {
        Map<String, List<LocationNode>> childrenByParentCode = new HashMap<>();
        for (LocationNode node : nodes) {
            if (node.getParent() == null) {
                continue;
            }
            String parentCode = node.getParent().getCode();
            childrenByParentCode
                    .computeIfAbsent(parentCode, ignored -> new ArrayList<>())
                    .add(node);
        }
        return childrenByParentCode;
    }

    private void collectDescendants(
            String code,
            Map<String, List<LocationNode>> childrenByParentCode,
            Set<String> subtreeCodes
    ) {
        List<LocationNode> children = childrenByParentCode.get(code);
        if (children == null) {
            return;
        }
        for (LocationNode child : children) {
            subtreeCodes.add(child.getCode());
            collectDescendants(child.getCode(), childrenByParentCode, subtreeCodes);
        }
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
