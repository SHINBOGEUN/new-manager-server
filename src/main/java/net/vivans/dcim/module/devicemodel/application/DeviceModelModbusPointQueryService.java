package net.vivans.dcim.module.devicemodel.application;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.common.application.UnitCodeResolver;
import net.vivans.dcim.module.common.application.CategoryCodeResolver;
import net.vivans.dcim.module.collectortask.application.CollectionScriptSyncService;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.common.domain.repository.CommonCodeRepository;
import net.vivans.dcim.module.devicemodel.api.dto.DeviceModelModbusPointCreateRequest;
import net.vivans.dcim.module.devicemodel.api.dto.DeviceModelModbusPointResponse;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModel;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModelModbusPoint;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModelProtocol;
import net.vivans.dcim.module.devicemodel.domain.repository.DeviceModelModbusPointRepository;
import net.vivans.dcim.module.devicemodel.domain.repository.DeviceModelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceModelModbusPointQueryService {

    private final DeviceModelRepository deviceModelRepository;
    private final DeviceModelModbusPointRepository deviceModelModbusPointRepository;
    private final CollectionScriptSyncService collectionScriptSyncService;
    private final UnitCodeResolver unitCodeResolver;
    private final CategoryCodeResolver categoryCodeResolver;
    private final CommonCodeRepository commonCodeRepository;

    public List<DeviceModelModbusPointResponse> getDeviceModelModbusPoints(Integer modelId, Integer protocolId) {
        findModbusProtocol(modelId, protocolId);

        List<DeviceModelModbusPoint> points =
                deviceModelModbusPointRepository.findAllByModelProtocolIdOrderByIdAsc(protocolId);

        List<DeviceModelModbusPointResponse> responses = new ArrayList<>();
        for (DeviceModelModbusPoint point : points) {
            responses.add(DeviceModelModbusPointResponse.from(point));
        }
        return responses;
    }

    public DeviceModelModbusPointResponse getDeviceModelModbusPoint(
            Integer modelId,
            Integer protocolId,
            Integer pointId
    ) {
        findModbusProtocol(modelId, protocolId);
        return DeviceModelModbusPointResponse.from(findModbusPoint(pointId, protocolId));
    }

    @Transactional
    public DeviceModelModbusPointResponse createDeviceModelModbusPoint(
            Integer modelId,
            Integer protocolId,
            DeviceModelModbusPointCreateRequest request
    ) {
        DeviceModelProtocol protocol = findModbusProtocol(modelId, protocolId);

        if (deviceModelModbusPointRepository.existsByModelProtocolIdAndName(protocolId, request.name())) {
            throw new IllegalArgumentException("point name already exists for this protocol");
        }

        boolean requiresInstance = Boolean.TRUE.equals(request.requiresInstance());
        boolean enabled = request.enabled() == null || request.enabled();

        DeviceModelModbusPoint point = DeviceModelModbusPoint.create(
                protocol,
                request.name(),
                resolveDataPointType(request.dataPointTypeId()),
                categoryCodeResolver.resolve(request.categoryCodeId()),
                request.registerType(),
                request.dataType(),
                request.byteOrder(),
                request.address(),
                requiresInstance,
                request.scale(),
                request.offset(),
                unitCodeResolver.resolve(request.unitCodeId()),
                enabled
        );

        point.setPhase(resolvePhase(request.phase()));

        DeviceModelModbusPoint saved = deviceModelModbusPointRepository.save(point);
        collectionScriptSyncService.regenerateByModelId(modelId);
        return DeviceModelModbusPointResponse.from(saved);
    }

    @Transactional
    public DeviceModelModbusPointResponse updateDeviceModelModbusPoint(
            Integer modelId,
            Integer protocolId,
            Integer pointId,
            DeviceModelModbusPointCreateRequest request
    ) {
        findModbusProtocol(modelId, protocolId);
        DeviceModelModbusPoint point = findModbusPoint(pointId, protocolId);

        if (deviceModelModbusPointRepository.existsByModelProtocolIdAndNameAndIdNot(
                protocolId, request.name(), pointId)) {
            throw new IllegalArgumentException("point name already exists for this protocol");
        }

        boolean requiresInstance = Boolean.TRUE.equals(request.requiresInstance());
        boolean enabled = request.enabled() == null || request.enabled();

        point.update(
                request.name(),
                resolveDataPointType(request.dataPointTypeId()),
                categoryCodeResolver.resolve(request.categoryCodeId()),
                request.registerType(),
                request.dataType(),
                request.byteOrder(),
                request.address(),
                requiresInstance,
                request.scale(),
                request.offset(),
                unitCodeResolver.resolve(request.unitCodeId()),
                enabled
        );

        point.setPhase(resolvePhase(request.phase()));

        DeviceModelModbusPoint saved = deviceModelModbusPointRepository.save(point);
        collectionScriptSyncService.regenerateByModelId(modelId);
        return DeviceModelModbusPointResponse.from(saved);
    }

    @Transactional
    public Integer deleteDeviceModelModbusPoint(Integer modelId, Integer protocolId, Integer pointId) {
        findModbusProtocol(modelId, protocolId);
        DeviceModelModbusPoint point = findModbusPoint(pointId, protocolId);
        deviceModelModbusPointRepository.delete(point);
        collectionScriptSyncService.regenerateByModelId(modelId);
        return pointId;
    }

    private DeviceModelProtocol findModbusProtocol(Integer modelId, Integer protocolId) {
        DeviceModel deviceModel = deviceModelRepository.findById(modelId)
                .orElseThrow(() -> new EntityNotFoundException("DeviceModel not found: " + modelId));

        for (DeviceModelProtocol protocol : deviceModel.getProtocols()) {
            if (protocolId.equals(protocol.getId())) {
                return protocol;
            }
        }
        throw new EntityNotFoundException("DeviceModelProtocol not found: " + protocolId);
    }

    private DeviceModelModbusPoint findModbusPoint(Integer pointId, Integer protocolId) {
        return deviceModelModbusPointRepository.findByIdAndModelProtocolId(pointId, protocolId)
                .orElseThrow(() -> new EntityNotFoundException("DeviceModelModbusPoint not found: " + pointId));
    }

    private CommonCode resolvePhase(String code) {
        if (code == null || code.isBlank()) return null;
        String normalized = code.trim().toUpperCase(java.util.Locale.ROOT);
        return commonCodeRepository.findByCodeGroupGroupKeyAndCode("PHASE", normalized)
                .orElseThrow(() -> new IllegalArgumentException("phase must be a PHASE code (P1, P2, P3, N)"));
    }

    private CommonCode resolveDataPointType(Integer id) {
        if (id == null) {
            return commonCodeRepository.findByCodeGroupGroupKeyAndCode("DATA_POINT_TYPE", "UNCLASSIFIED")
                    .orElseThrow(() -> new EntityNotFoundException(
                            "DATA_POINT_TYPE/UNCLASSIFIED is not configured"));
        }
        CommonCode code = commonCodeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("CommonCode not found: " + id));
        if (!"DATA_POINT_TYPE".equals(code.getCodeGroup().getGroupKey())) {
            throw new IllegalArgumentException("dataPointTypeId must belong to DATA_POINT_TYPE");
        }
        return code;
    }
}
