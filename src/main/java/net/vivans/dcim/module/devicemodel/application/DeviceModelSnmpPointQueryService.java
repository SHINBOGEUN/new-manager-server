package net.vivans.dcim.module.devicemodel.application;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.collectortask.application.CollectionScriptSyncService;
import net.vivans.dcim.module.devicemodel.api.dto.DeviceModelSnmpPointBulkCreateRequest;
import net.vivans.dcim.module.devicemodel.api.dto.DeviceModelSnmpPointCreateRequest;
import net.vivans.dcim.module.devicemodel.api.dto.DeviceModelSnmpPointResponse;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModel;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModelProtocol;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModelSnmpPoint;
import net.vivans.dcim.module.devicemodel.domain.repository.DeviceModelRepository;
import net.vivans.dcim.module.devicemodel.domain.repository.DeviceModelSnmpPointRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceModelSnmpPointQueryService {

    private final DeviceModelRepository deviceModelRepository;
    private final DeviceModelSnmpPointRepository deviceModelSnmpPointRepository;
    private final CollectionScriptSyncService collectionScriptSyncService;

    public List<DeviceModelSnmpPointResponse> getDeviceModelSnmpPoints(Integer modelId, Integer protocolId) {
        findSnmpProtocol(modelId, protocolId);

        List<DeviceModelSnmpPoint> points =
                deviceModelSnmpPointRepository.findAllByModelProtocolIdOrderByIdAsc(protocolId);

        List<DeviceModelSnmpPointResponse> responses = new ArrayList<>();
        for (DeviceModelSnmpPoint point : points) {
            responses.add(DeviceModelSnmpPointResponse.from(point));
        }
        return responses;
    }

    public DeviceModelSnmpPointResponse getDeviceModelSnmpPoint(
            Integer modelId,
            Integer protocolId,
            Integer pointId
    ) {
        findSnmpProtocol(modelId, protocolId);
        return DeviceModelSnmpPointResponse.from(findSnmpPoint(pointId, protocolId));
    }

    @Transactional
    public DeviceModelSnmpPointResponse createDeviceModelSnmpPoint(
            Integer modelId,
            Integer protocolId,
            DeviceModelSnmpPointCreateRequest request
    ) {
        DeviceModelProtocol protocol = findSnmpProtocol(modelId, protocolId);
        validateUniqueNameAndOid(protocolId, request.name(), request.oid());
        DeviceModelSnmpPoint saved = deviceModelSnmpPointRepository.save(createPoint(protocol, request));
        collectionScriptSyncService.regenerateByModelId(modelId);
        return DeviceModelSnmpPointResponse.from(saved);
    }

    @Transactional
    public List<DeviceModelSnmpPointResponse> createDeviceModelSnmpPoints(
            Integer modelId,
            Integer protocolId,
            DeviceModelSnmpPointBulkCreateRequest request
    ) {
        DeviceModelProtocol protocol = findSnmpProtocol(modelId, protocolId);
        List<DeviceModelSnmpPointCreateRequest> points = request.points();
        validateBatchUniqueness(protocolId, points);

        List<DeviceModelSnmpPointResponse> responses = new ArrayList<>();
        for (DeviceModelSnmpPointCreateRequest pointRequest : points) {
            DeviceModelSnmpPoint saved = deviceModelSnmpPointRepository.save(createPoint(protocol, pointRequest));
            responses.add(DeviceModelSnmpPointResponse.from(saved));
        }
        collectionScriptSyncService.regenerateByModelId(modelId);
        return responses;
    }

    @Transactional
    public DeviceModelSnmpPointResponse updateDeviceModelSnmpPoint(
            Integer modelId,
            Integer protocolId,
            Integer pointId,
            DeviceModelSnmpPointCreateRequest request
    ) {
        findSnmpProtocol(modelId, protocolId);
        DeviceModelSnmpPoint point = findSnmpPoint(pointId, protocolId);

        if (deviceModelSnmpPointRepository.existsByModelProtocolIdAndNameAndIdNot(
                protocolId, request.name(), pointId)) {
            throw new IllegalArgumentException("point name already exists for this protocol");
        }
        if (deviceModelSnmpPointRepository.existsByModelProtocolIdAndOidAndIdNot(
                protocolId, request.oid(), pointId)) {
            throw new IllegalArgumentException("point oid already exists for this protocol");
        }

        boolean requiresInstance = Boolean.TRUE.equals(request.requiresInstance());
        boolean enabled = request.enabled() == null || request.enabled();

        point.update(request.name(), request.oid(), requiresInstance, request.unit(), request.scale(), enabled);

        DeviceModelSnmpPoint saved = deviceModelSnmpPointRepository.save(point);
        collectionScriptSyncService.regenerateByModelId(modelId);
        return DeviceModelSnmpPointResponse.from(saved);
    }

    @Transactional
    public Integer deleteDeviceModelSnmpPoint(Integer modelId, Integer protocolId, Integer pointId) {
        findSnmpProtocol(modelId, protocolId);
        DeviceModelSnmpPoint point = findSnmpPoint(pointId, protocolId);
        deviceModelSnmpPointRepository.delete(point);
        collectionScriptSyncService.regenerateByModelId(modelId);
        return pointId;
    }

    private DeviceModelProtocol findSnmpProtocol(Integer modelId, Integer protocolId) {
        DeviceModel deviceModel = deviceModelRepository.findById(modelId)
                .orElseThrow(() -> new EntityNotFoundException("DeviceModel not found: " + modelId));

        for (DeviceModelProtocol protocol : deviceModel.getProtocols()) {
            if (protocolId.equals(protocol.getId())) {
                return protocol;
            }
        }
        throw new EntityNotFoundException("DeviceModelProtocol not found: " + protocolId);
    }

    private DeviceModelSnmpPoint findSnmpPoint(Integer pointId, Integer protocolId) {
        return deviceModelSnmpPointRepository.findByIdAndModelProtocolId(pointId, protocolId)
                .orElseThrow(() -> new EntityNotFoundException("DeviceModelSnmpPoint not found: " + pointId));
    }

    private void validateBatchUniqueness(Integer protocolId, List<DeviceModelSnmpPointCreateRequest> points) {
        Set<String> names = new HashSet<>();
        Set<String> oids = new HashSet<>();
        for (DeviceModelSnmpPointCreateRequest point : points) {
            if (!names.add(point.name())) {
                throw new IllegalArgumentException("duplicate point name in request: " + point.name());
            }
            if (!oids.add(point.oid())) {
                throw new IllegalArgumentException("duplicate point oid in request: " + point.oid());
            }
            validateUniqueNameAndOid(protocolId, point.name(), point.oid());
        }
    }

    private void validateUniqueNameAndOid(Integer protocolId, String name, String oid) {
        if (deviceModelSnmpPointRepository.existsByModelProtocolIdAndName(protocolId, name)) {
            throw new IllegalArgumentException("point name already exists for this protocol");
        }
        if (deviceModelSnmpPointRepository.existsByModelProtocolIdAndOid(protocolId, oid)) {
            throw new IllegalArgumentException("point oid already exists for this protocol");
        }
    }

    private DeviceModelSnmpPoint createPoint(DeviceModelProtocol protocol, DeviceModelSnmpPointCreateRequest request) {
        boolean requiresInstance = Boolean.TRUE.equals(request.requiresInstance());
        boolean enabled = request.enabled() == null || request.enabled();
        return DeviceModelSnmpPoint.create(
                protocol,
                request.name(),
                request.oid(),
                requiresInstance,
                request.unit(),
                request.scale(),
                enabled
        );
    }
}
