package net.vivans.dcim.module.device.application;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.device.api.dto.PageWidgetCreateRequest;
import net.vivans.dcim.module.device.api.dto.PageWidgetPueCreateRequest;
import net.vivans.dcim.module.device.api.dto.PageWidgetPueSourceRequest;
import net.vivans.dcim.module.device.api.dto.PageWidgetPueUpdateRequest;
import net.vivans.dcim.module.device.api.dto.PageWidgetResponse;
import net.vivans.dcim.module.device.api.dto.PageWidgetUpdateRequest;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.model.PageWidget;
import net.vivans.dcim.module.device.domain.model.PageWidgetChartRangePreset;
import net.vivans.dcim.module.device.domain.model.PageWidgetQueryKind;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import net.vivans.dcim.module.device.domain.repository.PageWidgetRepository;
import net.vivans.dcim.module.devicemodel.domain.repository.DeviceModelSnmpPointRepository;
import net.vivans.dcim.module.pue.application.PueCollectorSyncService;
import net.vivans.dcim.module.pue.application.PuePowerPointValidator;
import net.vivans.dcim.module.pue.domain.model.PueDefinition;
import net.vivans.dcim.module.pue.domain.model.PueDefinitionSourceRole;
import net.vivans.dcim.module.pue.domain.repository.PueDefinitionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PuePageWidgetHandler {

    private final PageWidgetSpecializedSupport support;
    private final PageWidgetRepository pageWidgetRepository;
    private final DeviceRepository deviceRepository;
    private final DeviceModelSnmpPointRepository deviceModelSnmpPointRepository;
    private final PueDefinitionRepository pueDefinitionRepository;
    private final PueCollectorSyncService pueCollectorSyncService;

    @Transactional
    public PageWidgetResponse create(PageWidgetPueCreateRequest request) {
        CommonCode pageCode = support.findPageCode(request.pageCode());
        String name = request.name().trim();
        support.validateCreateName(pageCode, name);
        List<PueDefinition.SourceDefinition> sources = resolveSources(request.totalSources(), request.coolerSources());
        PueDefinition definition = pueDefinitionRepository.save(PueDefinition.create(name, null, true, sources));
        pueCollectorSyncService.sync(definition);
        PageWidget widget = PageWidget.createPue(pageCode, name,
                request.enabled() == null || request.enabled(), definition,
                request.rangePreset() == null || request.rangePreset().isBlank()
                        ? PageWidgetChartRangePreset.last_24h
                        : PageWidgetChartRangePreset.from(request.rangePreset()),
                request.freshnessMinutes());
        support.applyLayout(widget, request.layout());
        return PageWidgetResponse.from(pageWidgetRepository.save(widget));
    }

    @Transactional
    public PageWidgetResponse update(Integer id, PageWidgetPueUpdateRequest request) {
        PageWidget widget = support.findWidget(id);
        if (widget.getQueryKind() != PageWidgetQueryKind.pue) {
            throw new IllegalArgumentException("queryKind must be pue");
        }
        String name = request.name().trim();
        support.validateUpdateName(widget, name);
        List<PueDefinition.SourceDefinition> sources = resolveSources(request.totalSources(), request.coolerSources());
        PueDefinition definition = findDefinition(widget.getPueDefinitionId());
        definition.update(definition.getName(), definition.getCalculationCron(), sources);
        pueDefinitionRepository.save(definition);
        pueCollectorSyncService.sync(definition);
        widget.updatePue(name, request.enabled() == null ? widget.isEnabled() : request.enabled(), definition,
                request.rangePreset() == null || request.rangePreset().isBlank()
                        ? widget.getPueRangePreset()
                        : PageWidgetChartRangePreset.from(request.rangePreset()),
                request.freshnessMinutes());
        support.applyLayout(widget, request.layout());
        return PageWidgetResponse.from(pageWidgetRepository.save(widget));
    }

    @Transactional
    public PageWidgetResponse createLinked(CommonCode pageCode, PageWidgetCreateRequest request) {
        PueDefinition definition = findDefinition(request.pueDefinitionId());
        PageWidget widget = PageWidget.createPue(pageCode, request.name(),
                request.enabled() == null || request.enabled(), definition,
                PageWidgetChartRangePreset.from(request.pueRangePreset()), request.pueFreshnessMinutes());
        widget.updateDataFreshnessMinutes(request.dataFreshnessMinutes());
        support.applyLayout(widget, request.layout());
        return PageWidgetResponse.from(pageWidgetRepository.save(widget));
    }

    @Transactional
    public PageWidgetResponse updateLinked(PageWidget widget, PageWidgetUpdateRequest request) {
        if (widget.getQueryKind() != PageWidgetQueryKind.pue) {
            throw new IllegalArgumentException("PUE widget type cannot be changed from another widget type");
        }
        PueDefinition definition = findDefinition(request.pueDefinitionId());
        widget.updatePue(request.name().trim(),
                request.enabled() == null ? widget.isEnabled() : request.enabled(), definition,
                PageWidgetChartRangePreset.from(request.pueRangePreset()), request.pueFreshnessMinutes());
        widget.updateDataFreshnessMinutes(request.dataFreshnessMinutes());
        support.applyLayout(widget, request.layout());
        return PageWidgetResponse.from(pageWidgetRepository.save(widget));
    }

    private List<PueDefinition.SourceDefinition> resolveSources(
            List<PageWidgetPueSourceRequest> totalSources,
            List<PageWidgetPueSourceRequest> coolerSources
    ) {
        List<PueDefinition.SourceDefinition> sources = new ArrayList<>();
        addSources(sources, totalSources, PueDefinitionSourceRole.total);
        addSources(sources, coolerSources, PueDefinitionSourceRole.cooler);
        if (sources.size() > 200) {
            throw new IllegalArgumentException("PUE supports at most 200 sources");
        }
        validatePoints(sources);
        return sources;
    }

    private void addSources(
            List<PueDefinition.SourceDefinition> target,
            List<PageWidgetPueSourceRequest> requests,
            PueDefinitionSourceRole role
    ) {
        if (requests == null) {
            return;
        }
        for (PageWidgetPueSourceRequest source : requests) {
            Device device = deviceRepository.findById(source.deviceId())
                    .orElseThrow(() -> new EntityNotFoundException("Device not found: " + source.deviceId()));
            if (!device.isEnabled()) {
                throw new IllegalArgumentException("PUE source device is disabled: " + source.deviceId());
            }
            target.add(new PueDefinition.SourceDefinition(device, role, source.pointName()));
        }
    }

    private void validatePoints(List<PueDefinition.SourceDefinition> sources) {
        new PuePowerPointValidator(deviceModelSnmpPointRepository).validateWidgetSources(sources.stream()
                .map(source -> new PuePowerPointValidator.Source(source.device(), source.pointName()))
                .toList());
    }

    private PueDefinition findDefinition(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("pueDefinitionId is required for PUE widget");
        }
        return pueDefinitionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("PueDefinition not found: " + id));
    }
}
