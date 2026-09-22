package net.vivans.dcim.module.device.application;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.common.domain.repository.CommonCodeRepository;
import net.vivans.dcim.module.device.api.dto.PageWidgetCreateRequest;
import net.vivans.dcim.module.device.api.dto.PageWidgetEnabledRequest;
import net.vivans.dcim.module.device.api.dto.PageWidgetLayoutRequest;
import net.vivans.dcim.module.device.api.dto.PageWidgetLastSourceRequest;
import net.vivans.dcim.module.device.api.dto.PageWidgetPueCreateRequest;
import net.vivans.dcim.module.device.api.dto.PageWidgetPueSourceRequest;
import net.vivans.dcim.module.device.api.dto.PageWidgetPueUpdateRequest;
import net.vivans.dcim.module.device.api.dto.PageWidgetPageResponse;
import net.vivans.dcim.module.device.api.dto.PageWidgetPsychrometricCreateRequest;
import net.vivans.dcim.module.device.api.dto.PageWidgetPsychrometricSourceRequest;
import net.vivans.dcim.module.device.api.dto.PageWidgetPsychrometricUpdateRequest;
import net.vivans.dcim.module.device.api.dto.PageWidgetPowerDistributionCreateRequest;
import net.vivans.dcim.module.device.api.dto.PageWidgetPowerDistributionGroupRequest;
import net.vivans.dcim.module.device.api.dto.PageWidgetPowerDistributionSourceRequest;
import net.vivans.dcim.module.device.api.dto.PageWidgetPowerDistributionUpdateRequest;
import net.vivans.dcim.module.device.api.dto.PageWidgetResponse;
import net.vivans.dcim.module.device.api.dto.PageWidgetUpdateRequest;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.model.DevicePageCodes;
import net.vivans.dcim.module.device.domain.model.PageWidget;
import net.vivans.dcim.module.device.domain.model.PageWidgetChartRangePreset;
import net.vivans.dcim.module.device.domain.model.PageWidgetChartScope;
import net.vivans.dcim.module.device.domain.model.PageWidgetChartSeriesMode;
import net.vivans.dcim.module.device.domain.model.PageWidgetCountMode;
import net.vivans.dcim.module.device.domain.model.PageWidgetGroupBy;
import net.vivans.dcim.module.device.domain.model.PageWidgetOp;
import net.vivans.dcim.module.device.domain.model.PageWidgetPsychrometric;
import net.vivans.dcim.module.device.domain.model.PageWidgetPsychrometricSourceRole;
import net.vivans.dcim.module.device.domain.model.PageWidgetPowerDistribution;
import net.vivans.dcim.module.device.domain.model.PageWidgetQueryKind;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import net.vivans.dcim.module.device.domain.repository.PageWidgetRepository;
import net.vivans.dcim.module.devicegroup.domain.model.DeviceGroup;
import net.vivans.dcim.module.devicegroup.domain.repository.DeviceGroupRepository;
import net.vivans.dcim.module.devicemodel.domain.repository.DeviceModelRepository;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModelSnmpPoint;
import net.vivans.dcim.module.devicemodel.domain.repository.DeviceModelSnmpPointRepository;
import net.vivans.dcim.module.pue.domain.model.PueDefinition;
import net.vivans.dcim.module.pue.domain.model.PueDefinitionSourceRole;
import net.vivans.dcim.module.pue.domain.repository.PueDefinitionRepository;
import net.vivans.dcim.module.pue.application.PueCollectorSyncService;
import net.vivans.dcim.shared.exception.ConflictException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PageWidgetQueryService {

    private static final String DUPLICATE_NAME_MESSAGE = "widget name already exists on this page";

    private final PageWidgetRepository pageWidgetRepository;
    private final CommonCodeRepository commonCodeRepository;
    private final DeviceRepository deviceRepository;
    private final DeviceGroupRepository deviceGroupRepository;
    private final DeviceModelRepository deviceModelRepository;
    private final DeviceModelSnmpPointRepository deviceModelSnmpPointRepository;
    private final PueDefinitionRepository pueDefinitionRepository;
    private final PueCollectorSyncService pueCollectorSyncService;

    public List<PageWidgetPageResponse> getPages() {
        return commonCodeRepository.findAll().stream()
                .filter(code -> DevicePageCodes.DEVICE_PAGE_GROUP_KEY.equals(
                        code.getCodeGroup().getGroupKey()))
                .sorted((left, right) -> {
                    int sortOrder = Integer.compare(
                            left.getSortOrder() == null ? 0 : left.getSortOrder(),
                            right.getSortOrder() == null ? 0 : right.getSortOrder());
                    return sortOrder != 0
                            ? sortOrder
                            : left.getName().compareToIgnoreCase(right.getName());
                })
                .map(PageWidgetPageResponse::from)
                .toList();
    }

    public List<PageWidgetResponse> getWidgets(String pageCode, Boolean enabled) {
        CommonCode code = findPageCode(pageCode);
        List<PageWidget> widgets = pageWidgetRepository.findAllByPageCodeIdOrderByIdAsc(code.getId());
        List<PageWidgetResponse> responses = new ArrayList<>();
        for (PageWidget widget : widgets) {
            if (enabled != null && widget.isEnabled() != enabled) {
                continue;
            }
            responses.add(PageWidgetResponse.from(widget));
        }
        return responses;
    }

    public PageWidgetResponse getWidget(Integer id) {
        return PageWidgetResponse.from(findWidget(id));
    }

    @Transactional
    public PageWidgetResponse createWidget(PageWidgetCreateRequest request) {
        CommonCode pageCode = findPageCode(request.pageCode());
        if (pageWidgetRepository.existsByPageCodeIdAndName(pageCode.getId(), request.name().trim())) {
            throw new ConflictException(DUPLICATE_NAME_MESSAGE);
        }

        PageWidgetQueryKind kind = PageWidgetQueryKind.from(request.queryKind());
        if (kind == PageWidgetQueryKind.pue) {
            PueDefinition definition = findPueDefinition(request.pueDefinitionId());
            PageWidget widget = PageWidget.createPue(pageCode, request.name(), request.enabled() == null || request.enabled(),
                    definition, PageWidgetChartRangePreset.from(request.pueRangePreset()), request.pueFreshnessMinutes());
            widget.updateDataFreshnessMinutes(request.dataFreshnessMinutes());
            applyLayout(widget, request.layout());
            return PageWidgetResponse.from(pageWidgetRepository.save(widget));
        }
        if (kind == PageWidgetQueryKind.psychrometric) {
            throw new IllegalArgumentException("psychrometric widget must be created with /widgets/psychrometric");
        }
        if (kind == PageWidgetQueryKind.power_distribution) {
            throw new IllegalArgumentException("power distribution widget must be created with /widgets/power-distribution");
        }
        if (kind != PageWidgetQueryKind.last && request.lastSources() != null && !request.lastSources().isEmpty()) {
            throw new IllegalArgumentException("lastSources is only allowed for last");
        }
        if (kind == PageWidgetQueryKind.last && request.deviceGroupIds() != null && !request.deviceGroupIds().isEmpty()) {
            throw new IllegalArgumentException("deviceGroupIds supports aggregate and chart devices only");
        }
        if (kind == PageWidgetQueryKind.last && request.lastSources() != null) {
            List<PageWidget.LastSourceDefinition> lastSources = resolveLastSources(request.lastSources());
            PageWidget widget = PageWidget.createLast(
                    pageCode,
                    request.name(),
                    request.enabled() == null || request.enabled(),
                    lastSources
            );
            widget.updateDataFreshnessMinutes(request.dataFreshnessMinutes());
            applyLayout(widget, request.layout());
            return PageWidgetResponse.from(pageWidgetRepository.save(widget));
        }
        PageWidgetOp op = PageWidgetOp.from(request.op());
        boolean enabled = request.enabled() == null || request.enabled();
        List<Device> devices = resolveDevices(request.deviceIds(), kind, request.chartScope(), op);
        List<DeviceGroup> deviceGroups = resolveDeviceGroups(request.deviceGroupIds(), kind, request.chartScope());
        List<Device> targetDevices = mergeTargetDevices(devices, deviceGroups);
        List<Integer> modelIds = resolveModelIds(request.modelIds(), kind, request.chartScope());
        PageWidgetChartSeriesMode chartSeriesMode = PageWidgetChartSeriesMode.from(request.chartSeriesMode());
        validateChartPoints(kind, request.pointNames(), targetDevices, modelIds, chartSeriesMode);
        PageWidget widget = PageWidget.create(
                pageCode,
                request.name(),
                enabled,
                kind,
                op,
                PageWidgetGroupBy.from(request.groupBy()),
                PageWidgetChartRangePreset.from(request.aggregateRangePreset()),
                PageWidgetCountMode.from(request.countMode()),
                request.countModelId(),
                PageWidgetChartScope.from(request.chartScope()),
                chartSeriesMode,
                PageWidgetChartRangePreset.from(request.chartRangePreset()),
                request.chartWindow(),
                request.pointNames(),
                devices,
                deviceGroups,
                List.of(),
                modelIds
        );
        widget.updateDataFreshnessMinutes(request.dataFreshnessMinutes());
        applyLayout(widget, request.layout());
        return PageWidgetResponse.from(pageWidgetRepository.save(widget));
    }

    @Transactional
    public PageWidgetResponse createPueWidget(PageWidgetPueCreateRequest request) {
        CommonCode pageCode = findPageCode(request.pageCode());
        String name = request.name().trim();
        if (pageWidgetRepository.existsByPageCodeIdAndName(pageCode.getId(), name)) {
            throw new ConflictException(DUPLICATE_NAME_MESSAGE);
        }
        List<PueDefinition.SourceDefinition> sources = new ArrayList<>();
        addPueSources(sources, request.totalSources(), PueDefinitionSourceRole.total);
        addPueSources(sources, request.coolerSources(), PueDefinitionSourceRole.cooler);
        if (sources.size() > 200) throw new IllegalArgumentException("PUE supports at most 200 sources");
        validatePuePoints(sources);
        PueDefinition definition = pueDefinitionRepository.save(PueDefinition.create(name, null, true, sources));
        pueCollectorSyncService.sync(definition);
        PageWidget widget = PageWidget.createPue(pageCode, name,
                request.enabled() == null || request.enabled(),
                definition,
                request.rangePreset() == null || request.rangePreset().isBlank()
                        ? PageWidgetChartRangePreset.last_24h
                        : PageWidgetChartRangePreset.from(request.rangePreset()),
                request.freshnessMinutes());
        applyLayout(widget, request.layout());
        return PageWidgetResponse.from(pageWidgetRepository.save(widget));
    }

    @Transactional
    public PageWidgetResponse updatePueWidget(Integer id, PageWidgetPueUpdateRequest request) {
        PageWidget widget = findWidget(id);
        if (widget.getQueryKind() != PageWidgetQueryKind.pue) {
            throw new IllegalArgumentException("queryKind must be pue");
        }
        String name = request.name().trim();
        if (pageWidgetRepository.existsByPageCodeIdAndNameAndIdNot(widget.getPageCode().getId(), name, id)) {
            throw new ConflictException(DUPLICATE_NAME_MESSAGE);
        }
        List<PueDefinition.SourceDefinition> sources = new ArrayList<>();
        addPueSources(sources, request.totalSources(), PueDefinitionSourceRole.total);
        addPueSources(sources, request.coolerSources(), PueDefinitionSourceRole.cooler);
        if (sources.size() > 200) throw new IllegalArgumentException("PUE supports at most 200 sources");
        validatePuePoints(sources);
        PueDefinition definition = findPueDefinition(widget.getPueDefinitionId());
        definition.update(definition.getName(), definition.getCalculationCron(), sources);
        pueDefinitionRepository.save(definition);
        pueCollectorSyncService.sync(definition);
        widget.updatePue(name, request.enabled() == null ? widget.isEnabled() : request.enabled(), definition,
                request.rangePreset() == null || request.rangePreset().isBlank()
                        ? widget.getPueRangePreset() : PageWidgetChartRangePreset.from(request.rangePreset()),
                request.freshnessMinutes());
        if (request.layout() != null) applyLayout(widget, request.layout());
        return PageWidgetResponse.from(pageWidgetRepository.save(widget));
    }

    @Transactional
    public PageWidgetResponse createPsychrometricWidget(PageWidgetPsychrometricCreateRequest request) {
        CommonCode pageCode = findPageCode(request.pageCode());
        String name = request.name().trim();
        if (pageWidgetRepository.existsByPageCodeIdAndName(pageCode.getId(), name)) {
            throw new ConflictException(DUPLICATE_NAME_MESSAGE);
        }
        List<PageWidgetPsychrometric.SourceDefinition> sources = new ArrayList<>();
        addPsychrometricSources(sources, request.temperatureSources(), PageWidgetPsychrometricSourceRole.temperature);
        addPsychrometricSources(sources, request.humiditySources(), PageWidgetPsychrometricSourceRole.humidity);
        if (sources.size() > 200) {
            throw new IllegalArgumentException("psychrometric supports at most 200 sources");
        }
        PageWidget widget = PageWidget.createPsychrometric(
                pageCode,
                name,
                request.enabled() == null || request.enabled(),
                sources
        );
        widget.updateDataFreshnessMinutes(request.dataFreshnessMinutes());
        applyLayout(widget, request.layout());
        return PageWidgetResponse.from(pageWidgetRepository.save(widget));
    }

    @Transactional
    public PageWidgetResponse updatePsychrometricWidget(Integer id, PageWidgetPsychrometricUpdateRequest request) {
        PageWidget widget = findWidget(id);
        if (widget.getQueryKind() != PageWidgetQueryKind.psychrometric) {
            throw new IllegalArgumentException("queryKind must be psychrometric");
        }
        String name = request.name().trim();
        if (pageWidgetRepository.existsByPageCodeIdAndNameAndIdNot(widget.getPageCode().getId(), name, id)) {
            throw new ConflictException(DUPLICATE_NAME_MESSAGE);
        }
        List<PageWidgetPsychrometric.SourceDefinition> sources = new ArrayList<>();
        addPsychrometricSources(sources, request.temperatureSources(), PageWidgetPsychrometricSourceRole.temperature);
        addPsychrometricSources(sources, request.humiditySources(), PageWidgetPsychrometricSourceRole.humidity);
        if (sources.size() > 200) {
            throw new IllegalArgumentException("psychrometric supports at most 200 sources");
        }
        widget.updatePsychrometric(
                name,
                request.enabled() == null ? widget.isEnabled() : request.enabled(),
                sources
        );
        widget.updateDataFreshnessMinutes(request.dataFreshnessMinutes());
        if (request.layout() != null) {
            applyLayout(widget, request.layout());
        }
        return PageWidgetResponse.from(pageWidgetRepository.save(widget));
    }

    @Transactional
    public PageWidgetResponse createPowerDistributionWidget(PageWidgetPowerDistributionCreateRequest request) {
        CommonCode pageCode = findPageCode(request.pageCode());
        String name = request.name().trim();
        if (pageWidgetRepository.existsByPageCodeIdAndName(pageCode.getId(), name)) {
            throw new ConflictException(DUPLICATE_NAME_MESSAGE);
        }
        PageWidget widget = PageWidget.createPowerDistribution(
                pageCode, name, request.enabled() == null || request.enabled(),
                resolvePowerDistributionGroups(request.groups()));
        widget.updateDataFreshnessMinutes(request.dataFreshnessMinutes());
        applyLayout(widget, request.layout());
        return PageWidgetResponse.from(pageWidgetRepository.save(widget));
    }

    @Transactional
    public PageWidgetResponse updatePowerDistributionWidget(Integer id, PageWidgetPowerDistributionUpdateRequest request) {
        PageWidget widget = findWidget(id);
        if (widget.getQueryKind() != PageWidgetQueryKind.power_distribution) {
            throw new IllegalArgumentException("queryKind must be power_distribution");
        }
        String name = request.name().trim();
        if (pageWidgetRepository.existsByPageCodeIdAndNameAndIdNot(widget.getPageCode().getId(), name, id)) {
            throw new ConflictException(DUPLICATE_NAME_MESSAGE);
        }
        widget.updatePowerDistribution(name, request.enabled() == null ? widget.isEnabled() : request.enabled(),
                resolvePowerDistributionGroups(request.groups()));
        widget.updateDataFreshnessMinutes(request.dataFreshnessMinutes());
        if (request.layout() != null) applyLayout(widget, request.layout());
        return PageWidgetResponse.from(pageWidgetRepository.save(widget));
    }

    private void validatePuePoints(List<PueDefinition.SourceDefinition> sources) {
        Set<Integer> modelIds = new LinkedHashSet<>();
        for (var source : sources) modelIds.add(source.device().getDeviceModel().getId());
        Map<String, DeviceModelSnmpPoint> catalog = new HashMap<>();
        for (DeviceModelSnmpPoint point : deviceModelSnmpPointRepository.findAllEnabledByDeviceModelIds(modelIds)) {
            catalog.put(point.getModelProtocol().getDeviceModel().getId() + "|"
                    + point.getName().toUpperCase(Locale.ROOT), point);
        }
        String unit = null;
        for (var source : sources) {
            DeviceModelSnmpPoint point = catalog.get(source.device().getDeviceModel().getId() + "|"
                    + source.pointName().trim().toUpperCase(Locale.ROOT));
            if (point == null || point.getDataPointType() == null
                    || !"POWER".equalsIgnoreCase(point.getDataPointType().getCode())) {
                throw new IllegalArgumentException("PUE source must be an enabled POWER point: device "
                        + source.device().getId() + ", point " + source.pointName());
            }
            if (point.getUnit() == null || point.getUnit().isBlank()) {
                throw new IllegalArgumentException("PUE source unit is required: " + source.pointName());
            }
            if (unit == null) unit = point.getUnit().trim();
            else if (!unit.equalsIgnoreCase(point.getUnit().trim())) {
                throw new IllegalArgumentException("PUE source points must use the same unit");
            }
        }
    }

    private void addPueSources(
            List<PueDefinition.SourceDefinition> target,
            List<PageWidgetPueSourceRequest> requests,
            PueDefinitionSourceRole role
    ) {
        if (requests == null) return;
        for (PageWidgetPueSourceRequest source : requests) {
            Device device = deviceRepository.findById(source.deviceId())
                    .orElseThrow(() -> new EntityNotFoundException("Device not found: " + source.deviceId()));
            if (!device.isEnabled()) {
                throw new IllegalArgumentException("PUE source device is disabled: " + source.deviceId());
            }
            target.add(new PueDefinition.SourceDefinition(device, role, source.pointName()));
        }
    }

    private void addPsychrometricSources(
            List<PageWidgetPsychrometric.SourceDefinition> target,
            List<PageWidgetPsychrometricSourceRequest> requests,
            PageWidgetPsychrometricSourceRole role
    ) {
        if (requests == null) {
            return;
        }
        for (PageWidgetPsychrometricSourceRequest source : requests) {
            Device device = deviceRepository.findById(source.deviceId())
                    .orElseThrow(() -> new EntityNotFoundException("Device not found: " + source.deviceId()));
            if (!device.isEnabled()) {
                throw new IllegalArgumentException("psychrometric source device is disabled: " + source.deviceId());
            }
            target.add(new PageWidgetPsychrometric.SourceDefinition(device, role, source.pointName()));
        }
    }

    private List<PageWidgetPowerDistribution.GroupDefinition> resolvePowerDistributionGroups(
            List<PageWidgetPowerDistributionGroupRequest> requests
    ) {
        if (requests == null || requests.size() < 2) {
            throw new IllegalArgumentException("power distribution requires at least two groups");
        }
        if (requests.size() > 12) throw new IllegalArgumentException("power distribution supports at most 12 groups");

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
                resolvedSources.add(new ResolvedPowerSource(groupName, device, pointName));
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

    private record ResolvedPowerSource(String groupName, Device device, String pointName) {
    }

    private PueDefinition findPueDefinition(Integer id) {
        if (id == null) throw new IllegalArgumentException("pueDefinitionId is required for PUE widget");
        return pueDefinitionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("PueDefinition not found: " + id));
    }

    @Transactional
    public PageWidgetResponse updateWidget(Integer id, PageWidgetUpdateRequest request) {
        PageWidget widget = findWidget(id);
        String name = request.name().trim();
        if (pageWidgetRepository.existsByPageCodeIdAndNameAndIdNot(widget.getPageCode().getId(), name, id)) {
            throw new ConflictException(DUPLICATE_NAME_MESSAGE);
        }

        PageWidgetQueryKind kind = PageWidgetQueryKind.from(request.queryKind());
        if (kind == PageWidgetQueryKind.pue) {
            if (widget.getQueryKind() != PageWidgetQueryKind.pue) {
                throw new IllegalArgumentException("PUE widget type cannot be changed from another widget type");
            }
            PueDefinition definition = findPueDefinition(request.pueDefinitionId());
            widget.updatePue(
                    name,
                    request.enabled() == null ? widget.isEnabled() : request.enabled(),
                    definition,
                    PageWidgetChartRangePreset.from(request.pueRangePreset()),
                    request.pueFreshnessMinutes()
            );
            widget.updateDataFreshnessMinutes(request.dataFreshnessMinutes());
            if (request.layout() != null) {
                applyLayout(widget, request.layout());
            }
            return PageWidgetResponse.from(pageWidgetRepository.save(widget));
        }
        if (kind == PageWidgetQueryKind.psychrometric) {
            if (widget.getQueryKind() != PageWidgetQueryKind.psychrometric) {
                throw new IllegalArgumentException("psychrometric widget type cannot be changed from another widget type");
            }
            throw new IllegalArgumentException("psychrometric widget must be updated with /widgets/{id}/psychrometric");
        }
        if (kind == PageWidgetQueryKind.power_distribution) {
            if (widget.getQueryKind() != PageWidgetQueryKind.power_distribution) {
                throw new IllegalArgumentException("power distribution widget type cannot be changed from another widget type");
            }
            throw new IllegalArgumentException("power distribution widget must be updated with /widgets/{id}/power-distribution");
        }
        if (widget.getQueryKind() == PageWidgetQueryKind.psychrometric) {
            throw new IllegalArgumentException("psychrometric widget must be updated with /widgets/{id}/psychrometric");
        }
        if (widget.getQueryKind() == PageWidgetQueryKind.power_distribution) {
            throw new IllegalArgumentException("power distribution widget must be updated with /widgets/{id}/power-distribution");
        }
        if (kind != PageWidgetQueryKind.last && request.lastSources() != null && !request.lastSources().isEmpty()) {
            throw new IllegalArgumentException("lastSources is only allowed for last");
        }
        if (kind == PageWidgetQueryKind.last && request.deviceGroupIds() != null && !request.deviceGroupIds().isEmpty()) {
            throw new IllegalArgumentException("deviceGroupIds supports aggregate and chart devices only");
        }
        if (kind == PageWidgetQueryKind.last && request.lastSources() != null) {
            List<PageWidget.LastSourceDefinition> lastSources = resolveLastSources(request.lastSources());
            widget.updateLast(name, request.enabled() == null ? widget.isEnabled() : request.enabled(), lastSources);
            widget.updateDataFreshnessMinutes(request.dataFreshnessMinutes());
            if (request.layout() != null) {
                applyLayout(widget, request.layout());
            }
            return PageWidgetResponse.from(pageWidgetRepository.save(widget));
        }
        PageWidgetOp op = PageWidgetOp.from(request.op());
        boolean enabled = request.enabled() == null ? widget.isEnabled() : request.enabled();
        List<Device> devices = resolveDevices(request.deviceIds(), kind, request.chartScope(), op);
        List<DeviceGroup> deviceGroups = resolveDeviceGroups(request.deviceGroupIds(), kind, request.chartScope());
        List<Device> targetDevices = mergeTargetDevices(devices, deviceGroups);
        List<Integer> modelIds = resolveModelIds(request.modelIds(), kind, request.chartScope());
        PageWidgetChartSeriesMode chartSeriesMode = PageWidgetChartSeriesMode.from(request.chartSeriesMode());
        validateChartPoints(kind, request.pointNames(), targetDevices, modelIds, chartSeriesMode);
        widget.update(
                name,
                enabled,
                kind,
                op,
                PageWidgetGroupBy.from(request.groupBy()),
                PageWidgetChartRangePreset.from(request.aggregateRangePreset()),
                PageWidgetCountMode.from(request.countMode()),
                request.countModelId(),
                PageWidgetChartScope.from(request.chartScope()),
                chartSeriesMode,
                PageWidgetChartRangePreset.from(request.chartRangePreset()),
                request.chartWindow(),
                request.pointNames(),
                devices,
                deviceGroups,
                List.of(),
                modelIds
        );
        widget.updateDataFreshnessMinutes(request.dataFreshnessMinutes());
        if (request.layout() != null) {
            applyLayout(widget, request.layout());
        }
        return PageWidgetResponse.from(pageWidgetRepository.save(widget));
    }

    @Transactional
    public PageWidgetResponse setEnabled(Integer id, PageWidgetEnabledRequest request) {
        PageWidget widget = findWidget(id);
        widget.setEnabled(request.enabled());
        return PageWidgetResponse.from(pageWidgetRepository.save(widget));
    }

    @Transactional
    public PageWidgetResponse replaceLayout(Integer id, PageWidgetLayoutRequest request) {
        PageWidget widget = findWidget(id);
        applyLayout(widget, request);
        return PageWidgetResponse.from(pageWidgetRepository.save(widget));
    }

    @Transactional
    public Integer deleteWidget(Integer id) {
        PageWidget widget = findWidget(id);
        pageWidgetRepository.delete(widget);
        return id;
    }

    private static void applyLayout(PageWidget widget, PageWidgetLayoutRequest layout) {
        if (layout == null) {
            return;
        }
        widget.upsertLayout(layout.gridX(), layout.gridY(), layout.w(), layout.h());
    }

    private List<Device> resolveDevices(
            List<Integer> deviceIds,
            PageWidgetQueryKind queryKind,
            String chartScopeRaw,
            PageWidgetOp op
    ) {
        if (deviceIds == null || deviceIds.isEmpty()) {
            if (queryKind == PageWidgetQueryKind.count || queryKind == PageWidgetQueryKind.pue
                    || queryKind == PageWidgetQueryKind.psychrometric || queryKind == PageWidgetQueryKind.aggregate) {
                return List.of();
            }
            if (queryKind == PageWidgetQueryKind.chart
                    && PageWidgetChartScope.from(chartScopeRaw) == PageWidgetChartScope.models) {
                return List.of();
            }
            if (queryKind == PageWidgetQueryKind.chart
                    && (chartScopeRaw == null || chartScopeRaw.isBlank()
                    || PageWidgetChartScope.from(chartScopeRaw) == PageWidgetChartScope.devices)) {
                return List.of();
            }
            throw new IllegalArgumentException("deviceIds is required");
        }
        return loadDevices(deviceIds);
    }

    private List<DeviceGroup> resolveDeviceGroups(
            List<Integer> deviceGroupIds,
            PageWidgetQueryKind queryKind,
            String chartScopeRaw
    ) {
        if (deviceGroupIds == null || deviceGroupIds.isEmpty()) {
            return List.of();
        }
        boolean allowed = queryKind == PageWidgetQueryKind.aggregate
                || (queryKind == PageWidgetQueryKind.chart
                && PageWidgetChartScope.from(chartScopeRaw) == PageWidgetChartScope.devices);
        if (!allowed) {
            throw new IllegalArgumentException("deviceGroupIds supports aggregate and chart devices only");
        }
        Set<Integer> uniqueIds = new LinkedHashSet<>();
        List<DeviceGroup> groups = new ArrayList<>();
        for (Integer groupId : deviceGroupIds) {
            if (groupId == null || groupId <= 0) {
                throw new IllegalArgumentException("deviceGroupIds must contain positive integers");
            }
            if (uniqueIds.add(groupId)) {
                groups.add(deviceGroupRepository.findById(groupId)
                        .orElseThrow(() -> new EntityNotFoundException("DeviceGroup not found: " + groupId)));
            }
        }
        return groups;
    }

    private static List<Device> mergeTargetDevices(List<Device> devices, List<DeviceGroup> groups) {
        Map<Integer, Device> targets = new java.util.LinkedHashMap<>();
        for (Device device : devices) targets.putIfAbsent(device.getId(), device);
        for (DeviceGroup group : groups) {
            if (!group.isEnabled()) continue;
            for (Device device : group.getDevices()) targets.putIfAbsent(device.getId(), device);
        }
        return new ArrayList<>(targets.values());
    }

    private List<Device> loadDevices(List<Integer> deviceIds) {
        Set<Integer> uniqueIds = new LinkedHashSet<>();
        for (Integer deviceId : deviceIds) {
            if (deviceId == null || deviceId <= 0) {
                throw new IllegalArgumentException("deviceIds must contain positive integers");
            }
            uniqueIds.add(deviceId);
        }
        List<Device> devices = new ArrayList<>();
        for (Integer deviceId : uniqueIds) {
            devices.add(deviceRepository.findById(deviceId)
                    .orElseThrow(() -> new EntityNotFoundException("Device not found: " + deviceId)));
        }
        return devices;
    }

    private List<PageWidget.LastSourceDefinition> resolveLastSources(
            List<PageWidgetLastSourceRequest> requests
    ) {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("lastSources is required for last");
        }
        List<PageWidget.LastSourceDefinition> sources = new ArrayList<>();
        int pointCount = 0;
        for (PageWidgetLastSourceRequest request : requests) {
            if (request == null || request.deviceId() == null || request.deviceId() <= 0) {
                throw new IllegalArgumentException("lastSources deviceId is required");
            }
            Device device = deviceRepository.findById(request.deviceId())
                    .orElseThrow(() -> new EntityNotFoundException("Device not found: " + request.deviceId()));
            if (request.pointNames() == null || request.pointNames().isEmpty()) {
                throw new IllegalArgumentException("lastSources pointNames is required: device " + request.deviceId());
            }
            List<String> pointNames = new ArrayList<>();
            for (String pointName : request.pointNames()) {
                if (pointName == null || pointName.isBlank()) {
                    throw new IllegalArgumentException("lastSources pointName is required: device " + request.deviceId());
                }
                pointNames.add(pointName.trim());
                pointCount++;
            }
            sources.add(new PageWidget.LastSourceDefinition(device, pointNames));
        }
        if (pointCount > 500) {
            throw new IllegalArgumentException("last supports at most 500 device-point selections");
        }
        return sources;
    }

    private void validateChartPoints(
            PageWidgetQueryKind kind,
            List<String> pointNames,
            List<Device> devices,
            List<Integer> modelIds,
            PageWidgetChartSeriesMode chartSeriesMode
    ) {
        if (kind != PageWidgetQueryKind.chart || pointNames == null || pointNames.isEmpty()) {
            return;
        }
        Set<Integer> targetModelIds = new LinkedHashSet<>(modelIds);
        for (Device device : devices) {
            targetModelIds.add(device.getDeviceModel().getId());
        }
        if (targetModelIds.isEmpty()) {
            return;
        }
        Set<String> units = new LinkedHashSet<>();
        for (DeviceModelSnmpPoint point
                : deviceModelSnmpPointRepository.findAllEnabledByDeviceModelIds(targetModelIds)) {
            if (!pointNames.contains(point.getName())) {
                continue;
            }
            CommonCode dataPointType = point.getDataPointType();
            if (dataPointType != null && "ENERGY".equalsIgnoreCase(dataPointType.getCode())) {
                throw new IllegalArgumentException(
                        "누적 ENERGY 측정항목은 차트에서 사용할 수 없습니다. 사용량 집계 위젯을 사용하세요: "
                                + point.getName());
            }
            String unit = point.getUnit();
            if (unit != null && !unit.isBlank()) {
                units.add(unit.trim());
            }
        }
        if (units.size() > 2) {
            throw new IllegalArgumentException(
                    "하나의 차트에는 최대 두 단위의 측정항목만 사용할 수 있습니다. 선택된 단위: "
                            + String.join(", ", units));
        }
        if (units.size() > 1 && chartSeriesMode != null && chartSeriesMode != PageWidgetChartSeriesMode.per_device) {
            throw new IllegalArgumentException(
                    "두 단위 차트는 서로 다른 단위를 합산할 수 없으므로 장비별(per_device) 표시 방식만 사용할 수 있습니다");
        }
    }

    private List<Integer> resolveModelIds(
            List<Integer> modelIds,
            PageWidgetQueryKind queryKind,
            String chartScopeRaw
    ) {
        if (queryKind != PageWidgetQueryKind.chart
                || PageWidgetChartScope.from(chartScopeRaw) != PageWidgetChartScope.models) {
            return List.of();
        }
        if (modelIds == null || modelIds.isEmpty()) {
            return List.of();
        }
        Set<Integer> uniqueIds = new LinkedHashSet<>();
        for (Integer modelId : modelIds) {
            if (modelId == null || modelId <= 0) {
                throw new IllegalArgumentException("modelIds must contain positive integers");
            }
            uniqueIds.add(modelId);
        }
        List<Integer> resolved = new ArrayList<>();
        for (Integer modelId : uniqueIds) {
            deviceModelRepository.findById(modelId)
                    .orElseThrow(() -> new EntityNotFoundException("DeviceModel not found: " + modelId));
            resolved.add(modelId);
        }
        return resolved;
    }

    private PageWidget findWidget(Integer id) {
        return pageWidgetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("PageWidget not found: " + id));
    }

    private CommonCode findPageCode(String pageCode) {
        if (pageCode == null || pageCode.isBlank()) {
            throw new IllegalArgumentException("pageCode is required");
        }
        return commonCodeRepository.findByCodeGroupGroupKeyAndCode(
                        DevicePageCodes.DEVICE_PAGE_GROUP_KEY,
                        pageCode.trim()
                )
                .orElseThrow(() -> new EntityNotFoundException("DEVICE_PAGE code not found: " + pageCode.trim()));
    }
}
