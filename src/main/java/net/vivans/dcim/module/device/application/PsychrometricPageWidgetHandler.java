package net.vivans.dcim.module.device.application;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.device.api.dto.PageWidgetPsychrometricCreateRequest;
import net.vivans.dcim.module.device.api.dto.PageWidgetPsychrometricSourceRequest;
import net.vivans.dcim.module.device.api.dto.PageWidgetPsychrometricUpdateRequest;
import net.vivans.dcim.module.device.api.dto.PageWidgetResponse;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.model.PageWidget;
import net.vivans.dcim.module.device.domain.model.PageWidgetPsychrometric;
import net.vivans.dcim.module.device.domain.model.PageWidgetPsychrometricSourceRole;
import net.vivans.dcim.module.device.domain.model.PageWidgetQueryKind;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import net.vivans.dcim.module.device.domain.repository.PageWidgetRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PsychrometricPageWidgetHandler {

    private final PageWidgetSpecializedSupport support;
    private final PageWidgetRepository pageWidgetRepository;
    private final DeviceRepository deviceRepository;

    @Transactional
    public PageWidgetResponse create(PageWidgetPsychrometricCreateRequest request) {
        CommonCode pageCode = support.findPageCode(request.pageCode());
        String name = request.name().trim();
        support.validateCreateName(pageCode, name);
        List<PageWidgetPsychrometric.SourceDefinition> sources = resolveSources(
                request.temperatureSources(), request.humiditySources());
        PageWidget widget = PageWidget.createPsychrometric(
                pageCode, name, request.enabled() == null || request.enabled(), sources);
        widget.updateDataFreshnessMinutes(request.dataFreshnessMinutes());
        support.applyLayout(widget, request.layout());
        return PageWidgetResponse.from(pageWidgetRepository.save(widget));
    }

    @Transactional
    public PageWidgetResponse update(Integer id, PageWidgetPsychrometricUpdateRequest request) {
        PageWidget widget = support.findWidget(id);
        if (widget.getQueryKind() != PageWidgetQueryKind.psychrometric) {
            throw new IllegalArgumentException("queryKind must be psychrometric");
        }
        String name = request.name().trim();
        support.validateUpdateName(widget, name);
        widget.updatePsychrometric(name,
                request.enabled() == null ? widget.isEnabled() : request.enabled(),
                resolveSources(request.temperatureSources(), request.humiditySources()));
        widget.updateDataFreshnessMinutes(request.dataFreshnessMinutes());
        support.applyLayout(widget, request.layout());
        return PageWidgetResponse.from(pageWidgetRepository.save(widget));
    }

    private List<PageWidgetPsychrometric.SourceDefinition> resolveSources(
            List<PageWidgetPsychrometricSourceRequest> temperatureSources,
            List<PageWidgetPsychrometricSourceRequest> humiditySources
    ) {
        List<PageWidgetPsychrometric.SourceDefinition> sources = new ArrayList<>();
        addSources(sources, temperatureSources, PageWidgetPsychrometricSourceRole.temperature);
        addSources(sources, humiditySources, PageWidgetPsychrometricSourceRole.humidity);
        if (sources.size() > 200) {
            throw new IllegalArgumentException("psychrometric supports at most 200 sources");
        }
        return sources;
    }

    private void addSources(
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
}
