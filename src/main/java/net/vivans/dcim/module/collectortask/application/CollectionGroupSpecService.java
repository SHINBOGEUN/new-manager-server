package net.vivans.dcim.module.collectortask.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.collectortask.domain.model.CollectionTask;
import net.vivans.dcim.module.collectortask.domain.model.CollectionTaskDevice;
import net.vivans.dcim.module.collectortask.domain.model.CollectionTaskGroup;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.model.DeviceProtocolEndpoint;
import net.vivans.dcim.module.device.domain.model.DeviceSnmpInstance;
import net.vivans.dcim.module.device.domain.repository.DeviceProtocolEndpointRepository;
import net.vivans.dcim.module.device.domain.repository.DeviceSnmpInstanceRepository;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModel;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModelProtocol;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModelSnmpPoint;
import net.vivans.dcim.module.devicemodel.domain.repository.DeviceModelSnmpPointRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CollectionGroupSpecService {

    public static final String SNMP_PROTOCOL_CODE = "snmp";
    private static final String DEFAULT_COMMUNITY = "public";
    private static final int DEFAULT_TIMEOUT_MS = 2000;
    private static final int DEFAULT_RETRIES = 1;
    private static final int DEFAULT_MAX_CONCURRENCY = 10;

    private final DeviceModelSnmpPointRepository deviceModelSnmpPointRepository;
    private final DeviceProtocolEndpointRepository deviceProtocolEndpointRepository;
    private final DeviceSnmpInstanceRepository deviceSnmpInstanceRepository;
    private final ObjectMapper objectMapper;

    public String generateJson(CollectionTaskGroup group) {
        try {
            return objectMapper.writeValueAsString(generate(group));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("failed to serialize collection group spec", e);
        }
    }

    public CollectionGroupSpec generate(CollectionTaskGroup group) {
        CollectionTask task = group.getTask();
        List<String> skipped = new ArrayList<>();

        if (!SNMP_PROTOCOL_CODE.equalsIgnoreCase(task.getScriptType().getCode())) {
            skipped.add("scriptType '" + task.getScriptType().getCode() + "' is not supported yet");
            return emptySpec(group, skipped);
        }

        DeviceModelProtocol snmpProtocol = findSnmpProtocol(task.getDeviceModel());
        if (snmpProtocol == null) {
            skipped.add("model has no SNMP protocol");
            return emptySpec(group, skipped);
        }

        List<CollectionGroupOidSpec> oids = new ArrayList<>();
        boolean requiresAnyInstance = false;
        for (DeviceModelSnmpPoint point : deviceModelSnmpPointRepository
                .findAllByModelProtocolIdOrderByIdAsc(snmpProtocol.getId())) {
            if (!point.isEnabled()) {
                skipped.add("point '" + point.getName() + "' skipped: disabled");
                continue;
            }
            oids.add(new CollectionGroupOidSpec(
                    point.getName(),
                    point.getOid(),
                    point.isRequiresInstance(),
                    point.getScale()
            ));
            if (point.isRequiresInstance()) {
                requiresAnyInstance = true;
            }
        }
        if (oids.isEmpty()) {
            skipped.add("no collectible SNMP points");
        }

        List<CollectionGroupTargetSpec> targets = new ArrayList<>();
        if (!oids.isEmpty()) {
            for (CollectionTaskDevice mapping : group.getDevices()) {
                CollectionGroupTargetSpec target = toTarget(mapping.getDevice(), requiresAnyInstance, skipped);
                if (target != null) {
                    targets.add(target);
                }
            }
        }
        if (targets.isEmpty()) {
            skipped.add("no collectible devices in group");
        }

        return new CollectionGroupSpec(
                task.getId(),
                group.getId(),
                task.getDeviceModel().getId(),
                SNMP_PROTOCOL_CODE,
                group.getCronExpression(),
                DEFAULT_COMMUNITY,
                DEFAULT_TIMEOUT_MS,
                DEFAULT_RETRIES,
                DEFAULT_MAX_CONCURRENCY,
                oids,
                targets,
                skipped
        );
    }

    private CollectionGroupTargetSpec toTarget(Device device, boolean requiresAnyInstance, List<String> skipped) {
        if (!device.isEnabled()) {
            skipped.add("device:" + device.getId() + " " + device.getName() + " - disabled");
            return null;
        }
        DeviceProtocolEndpoint endpoint = findEnabledSnmpEndpoint(device.getId());
        if (endpoint == null) {
            skipped.add("device:" + device.getId() + " " + device.getName() + " - no enabled SNMP endpoint");
            return null;
        }
        Integer instanceId = deviceSnmpInstanceRepository.findByEndpointId(endpoint.getId())
                .map(DeviceSnmpInstance::getInstanceId)
                .orElse(null);
        if (requiresAnyInstance && instanceId == null) {
            skipped.add("device:" + device.getId() + " " + device.getName() + " - missing SNMP instance");
            return null;
        }
        return new CollectionGroupTargetSpec(device.getId(), endpoint.getHost(), endpoint.getPort(), instanceId);
    }

    private DeviceProtocolEndpoint findEnabledSnmpEndpoint(Integer deviceId) {
        for (DeviceProtocolEndpoint endpoint : deviceProtocolEndpointRepository.findAllByDeviceIdOrderByIdAsc(deviceId)) {
            if (endpoint.isEnabled() && SNMP_PROTOCOL_CODE.equals(endpoint.getProtocolType().getCode())) {
                return endpoint;
            }
        }
        return null;
    }

    private static DeviceModelProtocol findSnmpProtocol(DeviceModel deviceModel) {
        for (DeviceModelProtocol protocol : deviceModel.getProtocols()) {
            if (SNMP_PROTOCOL_CODE.equals(protocol.getProtocolType().getCode())) {
                return protocol;
            }
        }
        return null;
    }

    private CollectionGroupSpec emptySpec(CollectionTaskGroup group, List<String> skipped) {
        return new CollectionGroupSpec(
                group.getTask().getId(),
                group.getId(),
                group.getTask().getDeviceModel().getId(),
                group.getTask().getScriptType().getCode(),
                group.getCronExpression(),
                DEFAULT_COMMUNITY,
                DEFAULT_TIMEOUT_MS,
                DEFAULT_RETRIES,
                DEFAULT_MAX_CONCURRENCY,
                List.of(),
                List.of(),
                skipped
        );
    }
}
