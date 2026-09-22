package net.vivans.dcim.module.device.application;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.device.api.dto.PageWidgetPowerDistributionCreateRequest;
import net.vivans.dcim.module.device.api.dto.PageWidgetPowerDistributionGroupRequest;
import net.vivans.dcim.module.device.api.dto.PageWidgetPowerDistributionSourceRequest;
import net.vivans.dcim.module.device.api.dto.PageWidgetPowerDistributionUpdateRequest;
import net.vivans.dcim.module.device.api.dto.PageWidgetResponse;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.model.PageWidget;
import net.vivans.dcim.module.device.domain.model.PageWidgetPowerDistribution;
import net.vivans.dcim.module.device.domain.model.PageWidgetQueryKind;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import net.vivans.dcim.module.device.domain.repository.PageWidgetRepository;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModelSnmpPoint;
import net.vivans.dcim.module.devicemodel.domain.repository.DeviceModelSnmpPointRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class PowerDistributionPageWidgetHandler {

    private final PageWidgetSpecializedSupport support;
    private final PageWidgetRepository pageWidgetRepository;
    private final DeviceRepository deviceRepository;
    private final DeviceModelSnmpPointRepository deviceModelSnmpPointRepository;

    @Transactional
    public PageWidgetResponse create(PageWidgetPowerDistributionCreateRequest request) {
        CommonCode pageCode = support.findPageCode(request.pageCode());
        String name = request.name().trim();
        support.validateCreateName(pageCode, name);
        PageWidget widget = PageWidget.createPowerDistribution(
                pageCode, name, request.enabled() == null || request.enabled(), resolveGroups(request.groups()));
        widget.updateDataFreshnessMinutes(request.dataFreshnessMinutes());
        support.applyLayout(widget, request.layout());
        return PageWidgetResponse.from(pageWidgetRepository.save(widget));
    }

    @Transactional
    public PageWidgetResponse update(Integer id, PageWidgetPowerDistributionUpdateRequest request) {
        PageWidget widget = support.findWidget(id);
        if (widget.getQueryKind() != PageWidgetQueryKind.power_distribution) {
            throw new IllegalArgumentException("queryKind must be power_distribution");
        }
        String name = request.name().trim();
        support.validateUpdateName(widget, name);
        widget.updatePowerDistribution(name,
                request.enabled() == null ? widget.isEnabled() : request.enabled(), resolveGroups(request.groups()));
        widget.updateDataFreshnessMinutes(request.dataFreshnessMinutes());
        support.applyLayout(widget, request.layout());
        return PageWidgetResponse.from(pageWidgetRepository.save(widget));
    }

    private List<PageWidgetPowerDistribution.GroupDefinition> resolveGroups(
            List<PageWidgetPowerDistributionGroupRequest> requests
    ) {
        if (requests == null || requests.size() < 2) {
            throw new IllegalArgumentException("power distribution requires at least two groups");
        }
        if (requests.size() > 12) {
            throw new IllegalArgumentException("power distribution supports at most 12 groups");
        }
        List<PageWidgetPowerDistribution.GroupDefinition> groups = new ArrayList<>();
        Set<String> uniqueNames = new LinkedHashSet<>();
        Set<String> uniqueSources = new LinkedHashSet<>();
        Set<Integer> modelIds = new LinkedHashSet<>();
        List<ResolvedPowerSource> resolvedSources = new ArrayList<>();
        for (PageWidgetPowerDistributionGroupRequest group : requests) {
            if (group == null || group.name() == null || group.name().isBlank()) {
                throw new IllegalArgumentException("power distribution group name is required");
            }
            String groupName = group.name().trim();
            if (!uniqueNames.add(groupName.toUpperCase(Locale.ROOT))) {
                throw new IllegalArgumentException("power distribution group names must be unique");
            }
            if (group.sources() == null || group.sources().isEmpty()) {
                throw new IllegalArgumentException("power distribution group sources are required: " + groupName);
            }
            List<PageWidgetPowerDistribution.SourceDefinition> sources = new ArrayList<>();
            for (PageWidgetPowerDistributionSourceRequest source : group.sources()) {
                if (source == null || source.deviceId() == null || source.deviceId() <= 0
                        || source.pointName() == null || source.pointName().isBlank()) {
                    throw new IllegalArgumentException("power distribution source is invalid: " + groupName);
                }
                Device device = deviceRepository.findById(source.deviceId())
                        .orElseThrow(() -> new EntityNotFoundException("Device not found: " + source.deviceId()));
                if (!device.isEnabled()) {
                    throw new IllegalArgumentException("power distribution source device is disabled: " + source.deviceId());
                }
                String pointName = source.pointName().trim();
                String sourceKey = device.getId() + "\u0000" + pointName.toUpperCase(Locale.ROOT);
                if (!uniqueSources.add(sourceKey)) {
                    throw new IllegalArgumentException("a power source can belong to only one group: device "
                            + device.getId() + ", point " + pointName);
                }
                modelIds.add(device.getDeviceModel().getId());
                resolvedSources.add(new ResolvedPowerSource(device, pointName));
                sources.add(new PageWidgetPowerDistribution.SourceDefinition(device, pointName));
            }
            groups.add(new PageWidgetPowerDistribution.GroupDefinition(groupName, group.color(), sources));
        }

        Map<String, DeviceModelSnmpPoint> catalog = new HashMap<>();
        for (DeviceModelSnmpPoint point : deviceModelSnmpPointRepository.findAllEnabledByDeviceModelIds(modelIds)) {
            catalog.put(point.getModelProtocol().getDeviceModel().getId() + "|"
                    + point.getName().toUpperCase(Locale.ROOT), point);
        }
        for (ResolvedPowerSource source : resolvedSources) {
            DeviceModelSnmpPoint point = catalog.get(source.device().getDeviceModel().getId() + "|"
                    + source.pointName().toUpperCase(Locale.ROOT));
            if (point == null || point.getDataPointType() == null
                    || !"POWER".equalsIgnoreCase(point.getDataPointType().getCode())) {
                throw new IllegalArgumentException("power distribution source must be an enabled POWER point: device "
                        + source.device().getId() + ", point " + source.pointName());
            }
            if (!"W".equalsIgnoreCase(point.getUnit() == null ? "" : point.getUnit().trim())) {
                throw new IllegalArgumentException("power distribution source unit must be W: device "
                        + source.device().getId() + ", point " + source.pointName());
            }
        }
        return groups;
    }

    private record ResolvedPowerSource(Device device, String pointName) {
    }
}
