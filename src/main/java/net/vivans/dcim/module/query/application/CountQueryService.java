package net.vivans.dcim.module.query.application;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.model.PageWidget;
import net.vivans.dcim.module.device.domain.model.PageWidgetCountMode;
import net.vivans.dcim.module.device.domain.model.PageWidgetQueryKind;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import net.vivans.dcim.module.device.domain.repository.PageWidgetRepository;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModel;
import net.vivans.dcim.module.query.api.dto.CountByModelResponse;
import net.vivans.dcim.module.query.api.dto.CountWidgetResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CountQueryService {

    private final PageWidgetRepository pageWidgetRepository;
    private final DeviceRepository deviceRepository;

    public CountWidgetResponse getCount(Integer widgetId, String countModeOverride, Integer countModelIdOverride) {
        PageWidget widget = findCountWidget(widgetId);
        PageWidgetCountMode mode = resolveMode(widget, countModeOverride);
        Integer filterModelId = resolveModelId(mode, widget, countModelIdOverride);

        List<Device> devices = deviceRepository.findAllEnabled();
        Map<Integer, ModelBucket> byModel = groupByModel(devices);

        return buildResponse(widget, mode, filterModelId, devices, byModel);
    }

    private CountWidgetResponse buildResponse(
            PageWidget widget,
            PageWidgetCountMode mode,
            Integer filterModelId,
            List<Device> devices,
            Map<Integer, ModelBucket> byModel
    ) {
        return switch (mode) {
            case total -> new CountWidgetResponse(
                    widget.getId(),
                    widget.getName(),
                    widget.getPageCode().getCode(),
                    mode.name(),
                    null,
                    devices.size(),
                    List.of()
            );
            case by_model -> new CountWidgetResponse(
                    widget.getId(),
                    widget.getName(),
                    widget.getPageCode().getCode(),
                    mode.name(),
                    null,
                    devices.size(),
                    toModelResponses(byModel)
            );
            case model -> {
                ModelBucket bucket = byModel.get(filterModelId);
                int count = bucket == null ? 0 : bucket.count();
                List<CountByModelResponse> rows = bucket == null
                        ? List.of()
                        : List.of(bucket.toResponse());
                yield new CountWidgetResponse(
                        widget.getId(),
                        widget.getName(),
                        widget.getPageCode().getCode(),
                        mode.name(),
                        filterModelId,
                        count,
                        rows
                );
            }
        };
    }

    private static PageWidgetCountMode resolveMode(PageWidget widget, String override) {
        if (override != null && !override.isBlank()) {
            return PageWidgetCountMode.from(override);
        }
        PageWidgetCountMode stored = widget.getCountMode();
        return stored == null ? PageWidgetCountMode.by_model : stored;
    }

    private static Integer resolveModelId(
            PageWidgetCountMode mode,
            PageWidget widget,
            Integer override
    ) {
        if (mode != PageWidgetCountMode.model) {
            return null;
        }
        Integer modelId = override != null ? override : widget.getCountModelId();
        if (modelId == null) {
            throw new IllegalArgumentException("countModelId is required when countMode is model");
        }
        return modelId;
    }

    private static Map<Integer, ModelBucket> groupByModel(List<Device> devices) {
        Map<Integer, ModelBucket> byModel = new LinkedHashMap<>();
        for (Device device : devices) {
            DeviceModel model = device.getDeviceModel();
            byModel.computeIfAbsent(model.getId(), id -> new ModelBucket(model))
                    .increment();
        }
        return byModel;
    }

    private static List<CountByModelResponse> toModelResponses(Map<Integer, ModelBucket> byModel) {
        return byModel.values().stream()
                .sorted(Comparator.comparing(ModelBucket::modelId))
                .map(ModelBucket::toResponse)
                .toList();
    }

    private PageWidget findCountWidget(Integer widgetId) {
        if (widgetId == null) {
            throw new IllegalArgumentException("widgetId is required");
        }
        PageWidget widget = pageWidgetRepository.findById(widgetId)
                .orElseThrow(() -> new EntityNotFoundException("PageWidget not found: " + widgetId));
        if (widget.getQueryKind() != PageWidgetQueryKind.count) {
            throw new IllegalArgumentException(
                    "widget queryKind must be count, but was " + widget.getQueryKind());
        }
        if (!widget.isEnabled()) {
            throw new IllegalArgumentException("widget is disabled");
        }
        return widget;
    }

    private static final class ModelBucket {
        private final Integer modelId;
        private final String modelName;
        private final String manufacturer;
        private int count;

        private ModelBucket(DeviceModel model) {
            this.modelId = model.getId();
            this.modelName = model.getName();
            this.manufacturer = model.getManufacturer();
            this.count = 0;
        }

        private void increment() {
            count++;
        }

        private int count() {
            return count;
        }

        private Integer modelId() {
            return modelId;
        }

        private CountByModelResponse toResponse() {
            return new CountByModelResponse(modelId, modelName, manufacturer, count);
        }
    }
}
