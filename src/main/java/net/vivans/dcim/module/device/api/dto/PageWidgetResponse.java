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
        String aggregateRangePreset,
        String countMode,
        Integer countModelId,
        String chartScope,
        String chartSeriesMode,
        String chartRangePreset,
        String chartWindow,
        String pueRangePreset,
        Integer pueFreshnessMinutes,
        Integer pueDefinitionId,
        List<PageWidgetPueSourceResponse> pueSources,
        List<PageWidgetLastSourceResponse> lastSources,
        List<PageWidgetPsychrometricSourceResponse> psychrometricSources,
        List<PageWidgetPowerDistributionGroupResponse> powerDistributionGroups,
        List<Integer> deviceIds,
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
                widget.getAggregateRangePreset() == null ? null : widget.getAggregateRangePreset().name(),
                widget.getCountMode() == null ? null : widget.getCountMode().name(),
                widget.getCountModelId(),
                widget.getChartScope() == null ? null : widget.getChartScope().name(),
                widget.getChartSeriesMode() == null ? null : widget.getChartSeriesMode().name(),
                widget.getChartRangePreset() == null ? null : widget.getChartRangePreset().name(),
                widget.getChartWindow(),
                widget.getPueRangePreset() == null ? null : widget.getPueRangePreset().name(),
                widget.getPueFreshnessMinutes(),
                widget.getPueDefinitionId(),
                widget.getPue() == null || widget.getPue().getPueDefinition() == null
                        ? List.of()
                        : widget.getPue().getPueDefinition().getSources().stream()
                        .map(PageWidgetPueSourceResponse::from)
                        .toList(),
                widget.getQueryKind().name().equals("last")
                        ? widget.lastSourceDefinitions().stream()
                        .map(PageWidgetLastSourceResponse::from)
                        .toList()
                        : List.of(),
                widget.getPsychrometric() == null
                        ? List.of()
                        : widget.getPsychrometric().getSources().stream()
                        .map(PageWidgetPsychrometricSourceResponse::from)
                        .toList(),
                widget.getPowerDistribution() == null
                        ? List.of()
                        : widget.getPowerDistribution().getGroups().stream()
                        .map(PageWidgetPowerDistributionGroupResponse::from)
                        .toList(),
                widget.deviceIds(),
                widget.modelIds(),
                widget.pointNames(),
                PageWidgetLayoutResponse.from(widget.getLayout())
        );
    }
}
