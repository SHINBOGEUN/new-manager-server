package net.vivans.dcim.module.device.api.dto;

import net.vivans.dcim.module.device.domain.model.PageWidget;

import java.util.List;

public record PageWidgetResponse(
        Integer id,
        Integer pageCodeId,
        String pageCode,
        String pageName,
        String name,
        boolean enabled,
        String queryKind,
        String op,
        String groupBy,
        String weightPoint,
        String numeratorPoint,
        String denominatorPoint,
        String aggregateRangePreset,
        String countMode,
        Integer countModelId,
        String chartScope,
        String chartSeriesMode,
        String chartRangePreset,
        String chartWindow,
        List<Integer> deviceIds,
        List<Integer> itDeviceIds,
        List<Integer> modelIds,
        List<String> pointNames,
        PageWidgetLayoutResponse layout
) {

    public static PageWidgetResponse from(PageWidget widget) {
        return new PageWidgetResponse(
                widget.getId(),
                widget.getPageCode().getId(),
                widget.getPageCode().getCode(),
                widget.getPageCode().getName(),
                widget.getName(),
                widget.isEnabled(),
                widget.getQueryKind().name(),
                widget.getOp() == null ? null : widget.getOp().name(),
                widget.getGroupBy() == null ? null : widget.getGroupBy().name(),
                null,
                null,
                null,
                widget.getAggregateRangePreset() == null ? null : widget.getAggregateRangePreset().name(),
                widget.getCountMode() == null ? null : widget.getCountMode().name(),
                widget.getCountModelId(),
                widget.getChartScope() == null ? null : widget.getChartScope().name(),
                widget.getChartSeriesMode() == null ? null : widget.getChartSeriesMode().name(),
                widget.getChartRangePreset() == null ? null : widget.getChartRangePreset().name(),
                widget.getChartWindow(),
                widget.deviceIds(),
                widget.itDeviceIds(),
                widget.modelIds(),
                widget.pointNames(),
                PageWidgetLayoutResponse.from(widget.getLayout())
        );
    }
}
