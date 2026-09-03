package net.vivans.dcim.module.device.application;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.common.domain.repository.CommonCodeRepository;
import net.vivans.dcim.module.device.api.dto.PageWidgetCreateRequest;
import net.vivans.dcim.module.device.api.dto.PageWidgetEnabledRequest;
import net.vivans.dcim.module.device.api.dto.PageWidgetLayoutRequest;
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
import net.vivans.dcim.module.device.domain.model.PageWidgetQueryKind;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import net.vivans.dcim.module.device.domain.repository.PageWidgetRepository;
import net.vivans.dcim.module.devicemodel.domain.repository.DeviceModelRepository;
import net.vivans.dcim.shared.exception.ConflictException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PageWidgetQueryService {

    private static final String DUPLICATE_NAME_MESSAGE = "widget name already exists on this page";

    private final PageWidgetRepository pageWidgetRepository;
    private final CommonCodeRepository commonCodeRepository;
    private final DeviceRepository deviceRepository;
    private final DeviceModelRepository deviceModelRepository;

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
        PageWidgetOp op = PageWidgetOp.from(request.op());
        boolean enabled = request.enabled() == null || request.enabled();
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
                PageWidgetChartSeriesMode.from(request.chartSeriesMode()),
                PageWidgetChartRangePreset.from(request.chartRangePreset()),
                request.chartWindow(),
                request.pointNames(),
                resolveDevices(request.deviceIds(), kind, request.chartScope(), op),
                List.of(),
                resolveModelIds(request.modelIds(), kind, request.chartScope())
        );
        applyLayout(widget, request.layout());
        return PageWidgetResponse.from(pageWidgetRepository.save(widget));
    }

    @Transactional
    public PageWidgetResponse updateWidget(Integer id, PageWidgetUpdateRequest request) {
        PageWidget widget = findWidget(id);
        String name = request.name().trim();
        if (pageWidgetRepository.existsByPageCodeIdAndNameAndIdNot(widget.getPageCode().getId(), name, id)) {
            throw new ConflictException(DUPLICATE_NAME_MESSAGE);
        }

        PageWidgetQueryKind kind = PageWidgetQueryKind.from(request.queryKind());
        PageWidgetOp op = PageWidgetOp.from(request.op());
        boolean enabled = request.enabled() == null ? widget.isEnabled() : request.enabled();
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
                PageWidgetChartSeriesMode.from(request.chartSeriesMode()),
                PageWidgetChartRangePreset.from(request.chartRangePreset()),
                request.chartWindow(),
                request.pointNames(),
                resolveDevices(request.deviceIds(), kind, request.chartScope(), op),
                List.of(),
                resolveModelIds(request.modelIds(), kind, request.chartScope())
        );
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
            if (queryKind == PageWidgetQueryKind.count) {
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
