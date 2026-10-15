package net.vivans.dcim.module.device.application;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.common.domain.repository.CommonCodeRepository;
import net.vivans.dcim.module.device.api.dto.DevicePageOptionsResponse;
import net.vivans.dcim.module.device.api.dto.DevicePageOptionsResponse.DeviceOption;
import net.vivans.dcim.module.device.api.dto.DevicePageOptionsResponse.PointOption;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.model.DeviceModbusReading;
import net.vivans.dcim.module.device.domain.model.DevicePageCodes;
import net.vivans.dcim.module.device.domain.model.DevicePageDevice;
import net.vivans.dcim.module.device.domain.model.DevicePageModelPointSetting;
import net.vivans.dcim.module.device.domain.repository.DeviceModbusReadingRepository;
import net.vivans.dcim.module.device.infrastructure.persistence.DevicePageDeviceRepository;
import net.vivans.dcim.module.device.infrastructure.persistence.DeviceModbusBitFieldRepository;
import net.vivans.dcim.module.device.infrastructure.persistence.DevicePageModelPointSettingRepository;
import net.vivans.dcim.module.devicemodel.api.dto.DeviceModelMeasurementPointOptionsResponse;
import net.vivans.dcim.module.devicemodel.application.DeviceModelMeasurementPointQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DevicePageOptionsQueryService {
    private final CommonCodeRepository commonCodeRepository;
    private final DevicePageDeviceRepository pageDeviceRepository;
    private final DevicePageModelPointSettingRepository pointSettingRepository;
    private final DeviceModbusReadingRepository modbusReadingRepository;
    private final DeviceModbusBitFieldRepository bitFieldRepository;
    private final DeviceModelMeasurementPointQueryService pointQueryService;

    public DevicePageOptionsResponse getOptions(String pageCode) {
        CommonCode page = findPage(pageCode);
        List<DevicePageDevice> pageDevices = pageDeviceRepository
                .findAllByPageCode_IdOrderByIdAsc(page.getId());

        Map<Integer, Map<PointIdentity, DevicePageModelPointSetting>> settingsByModel = new HashMap<>();
        Map<Integer, LinkedHashMap<String, PointOption>> pointsByDevice = new LinkedHashMap<>();
        List<DeviceOption> devices = new ArrayList<>(pageDevices.size());
        for (DevicePageDevice pageDevice : pageDevices) {
            Device device = pageDevice.getDevice();
            Integer modelId = device.getDeviceModel().getId();
            Map<PointIdentity, DevicePageModelPointSetting> modelSettings = settingsByModel.computeIfAbsent(
                    modelId, ignored -> findSettingsByPoint(page, modelId));
            LinkedHashMap<String, PointOption> devicePoints = new LinkedHashMap<>();
            addVisibleModelPoints(devicePoints, modelId, modelSettings);
            pointsByDevice.put(device.getId(), devicePoints);
        }

        List<Integer> pageDeviceIds = pageDevices.stream()
                .map(pageDevice -> pageDevice.getDevice().getId()).toList();
        List<DeviceModbusReading> readings = pageDeviceIds.isEmpty()
                ? List.of()
                : modbusReadingRepository.findAllByTargetDeviceIds(pageDeviceIds);
        Map<Integer, DeviceModbusReading> visibleReadingsById = new HashMap<>();
        for (DeviceModbusReading reading : readings) {
            if (!reading.isEnabled() || !reading.getPoint().isEnabled()
                    || !reading.getEndpointModbus().getEndpoint().isEnabled()) {
                continue;
            }

            Integer sourceModelId = reading.getPoint().getModelProtocol().getDeviceModel().getId();
            Integer protocolTypeId = reading.getPoint().getModelProtocol().getProtocolType().getId();
            Map<PointIdentity, DevicePageModelPointSetting> sourceSettings = settingsByModel.computeIfAbsent(
                    sourceModelId, ignored -> findSettingsByPoint(page, sourceModelId));
            DevicePageModelPointSetting setting = sourceSettings.get(
                    new PointIdentity(protocolTypeId, reading.getPoint().getId()));
            if (setting == null || !setting.isVisible()) {
                continue;
            }

            PointOption option = new PointOption(
                    protocolTypeId,
                    "modbus",
                    reading.getPoint().getId(),
                    sourceModelId,
                    reading.getPointName(),
                    reading.getPoint().getUnit(),
                    reading.getPoint().getDataType().name(),
                    reading.getPoint().getDataPointType().getId(),
                    reading.getPoint().getDataPointType().getCode(),
                    reading.getPoint().getCategoryCode() == null ? null : reading.getPoint().getCategoryCode().getId(),
                    reading.getPoint().getCategoryCode() == null ? null : reading.getPoint().getCategoryCode().getCode(),
                    reading.getPoint().getCategoryCode() == null ? null : reading.getPoint().getCategoryCode().getName(),
                    true,
                    true,
                    "READING",
                    setting.getSortOrder()
            );
            addPoint(pointsByDevice.get(reading.getTargetDevice().getId()), option);
            visibleReadingsById.put(reading.getId(), reading);
        }

        if (!visibleReadingsById.isEmpty()) {
            List<net.vivans.dcim.module.device.domain.model.DeviceModbusBitField> bitFields =
                    bitFieldRepository.findAllByReading_IdInOrderByIdAsc(
                            List.copyOf(visibleReadingsById.keySet()));
            for (var bitField : bitFields) {
                DeviceModbusReading reading = visibleReadingsById.get(bitField.getReading().getId());
                if (reading == null) continue;
                Integer sourceModelId = reading.getPoint().getModelProtocol().getDeviceModel().getId();
                Integer protocolTypeId = reading.getPoint().getModelProtocol().getProtocolType().getId();
                DevicePageModelPointSetting setting = settingsByModel.get(sourceModelId)
                        .get(new PointIdentity(protocolTypeId, reading.getPoint().getId()));
                PointOption option = new PointOption(
                        protocolTypeId,
                        "modbus",
                        reading.getPoint().getId(),
                        sourceModelId,
                        bitField.getPointName(),
                        null,
                        "STATUS",
                        null,
                        null,
                        null,
                        null,
                        null,
                        true,
                        true,
                        "BIT_FIELD",
                        setting.getSortOrder()
                );
                addPoint(pointsByDevice.get(reading.getTargetDevice().getId()), option);
            }
        }

        for (DevicePageDevice pageDevice : pageDevices) {
            Device device = pageDevice.getDevice();
            List<PointOption> points = pointsByDevice.get(device.getId()).values().stream()
                    .sorted(Comparator.comparingInt(PointOption::sortOrder)
                            .thenComparing(PointOption::protocol)
                            .thenComparing(PointOption::pointName))
                    .toList();
            devices.add(new DeviceOption(
                    device.getId(), device.getName(), device.getDeviceModel().getDeviceType().getCode(),
                    device.getDeviceModel().getId(),
                    device.getDeviceModel().getName(), device.getLocationNode().getCode(),
                    device.getLocationNode().getName(), device.isEnabled(), points));
        }

        return new DevicePageOptionsResponse(page.getCode(), page.getName(), List.copyOf(devices));
    }

    private Map<PointIdentity, DevicePageModelPointSetting> findSettingsByPoint(CommonCode page, Integer modelId) {
        Map<PointIdentity, DevicePageModelPointSetting> settingsByPoint = new HashMap<>();
        for (DevicePageModelPointSetting setting : pointSettingRepository
                .findAllByPageCode_IdAndDeviceModel_IdOrderBySortOrderAscIdAsc(page.getId(), modelId)) {
            settingsByPoint.put(new PointIdentity(
                    setting.getProtocolType().getId(), setting.getPointId()), setting);
        }
        return settingsByPoint;
    }

    private void addVisibleModelPoints(LinkedHashMap<String, PointOption> points,
                                       Integer modelId,
                                       Map<PointIdentity, DevicePageModelPointSetting> settingsByPoint) {
        DeviceModelMeasurementPointOptionsResponse catalog = pointQueryService.getMeasurementPoints(modelId);
        for (DeviceModelMeasurementPointOptionsResponse.PointOption point : catalog.points()) {
            if ("modbus".equalsIgnoreCase(point.protocol())
                    && Boolean.TRUE.equals(point.requiresInstance())) {
                continue;
            }
            DevicePageModelPointSetting setting = settingsByPoint.get(identity(point));
            if (setting == null || !setting.isVisible()) {
                continue;
            }
            addPoint(points, new PointOption(
                    point.protocolTypeId(),
                    point.protocol(),
                    point.pointId(),
                    modelId,
                    point.pointName(),
                    point.unit(),
                    point.valueType(),
                    point.dataPointTypeId(),
                    point.dataPointType(),
                    point.categoryCodeId(),
                    point.categoryCode(),
                    point.categoryName(),
                    point.requiresInstance(),
                    point.collectionEnabled(),
                    "MODEL_POINT",
                    setting.getSortOrder(),
                    point.componentScope(),
                    point.componentType()
            ));
        }
    }

    private static void addPoint(LinkedHashMap<String, PointOption> points, PointOption point) {
        if (points == null) return;
        String key = point.protocol() + "\u0000" + point.pointName();
        PointOption existing = points.get(key);
        if (existing == null || "READING".equals(point.origin())) {
            points.put(key, point);
        }
    }

    private CommonCode findPage(String pageCode) {
        if (pageCode == null || pageCode.isBlank()) {
            throw new IllegalArgumentException("pageCode is required");
        }
        return commonCodeRepository.findByCodeGroupGroupKeyAndCode(
                        DevicePageCodes.DEVICE_PAGE_GROUP_KEY, pageCode.trim())
                .orElseThrow(() -> new EntityNotFoundException(
                        "DEVICE_PAGE code not found: " + pageCode.trim()));
    }

    private static PointIdentity identity(DeviceModelMeasurementPointOptionsResponse.PointOption point) {
        return new PointIdentity(point.protocolTypeId(), point.pointId());
    }

    private record PointIdentity(Integer protocolTypeId, Integer pointId) {
    }
}
