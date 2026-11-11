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
        String visualStyle,
        String visualColor,
        String trendRangePreset,
        String trendWindow,
        String tableColumnsJson,
        int dataFreshnessMinutes,
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
        List<PageWidgetCalculatedSourceResponse> pueSources,
        List<PageWidgetLastSourceResponse> lastSources,
        List<PageWidgetPsychrometricSourceResponse> psychrometricSources,
        List<PageWidgetPowerDistributionGroupResponse> powerDistributionGroups,
        List<CctvCameraResponse> cctvCameras,
        List<Integer> deviceIds,
        List<Integer> deviceGroupIds,
        List<Integer> modelIds,
        List<String> pointNames,
        PageWidgetLayoutResponse layout,
        java.util.Map<String, Object> clickConfig
) {

    private static final com.fasterxml.jackson.databind.ObjectMapper JSON = new com.fasterxml.jackson.databind.ObjectMapper();

    public static PageWidgetResponse from(PageWidget widget) {
        return new PageWidgetResponse(
                widget.getId(),
                widget.getPageCode().getId(),
                widget.getPageCode().getCode(),
                widget.getPageCode().getName(),
                widget.getName(),
                widget.isEnabled(),
                widget.getVisualStyle() == null ? "classic" : widget.getVisualStyle(),
                widget.getVisualColor(),
                widget.getTrendRangePreset(),
                widget.getTrendWindow(),
                widget.getTableColumnsJson(),
                widget.getDataFreshnessMinutes(),
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
                widget.getCalculatedRangePreset() == null ? null : widget.getCalculatedRangePreset().name(),
                widget.getCalculatedFreshnessMinutes(),
                widget.getCalculatedMetricId(),
                widget.getCalculated() == null || widget.getCalculated().getCalculatedMetric() == null
                        ? List.of()
                        : widget.getCalculated().getCalculatedMetric().calculatedSources().stream()
                            .map(PageWidgetCalculatedSourceResponse::from).toList(),
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
                widget.cctvCameraDefinitions().stream().map(CctvCameraResponse::from).toList(),
                widget.deviceIds(),
                widget.deviceGroupIds(),
                widget.modelIds(),
                widget.pointNames(),
                PageWidgetLayoutResponse.from(widget.getLayout()),
                resolveClickConfig(widget)
        );
    }

    private static java.util.Map<String, Object> resolveClickConfig(PageWidget widget) {
        if (widget.getClickConfig() != null && !widget.getClickConfig().isBlank()) {
            try {
                return JSON.readValue(widget.getClickConfig(), new com.fasterxml.jackson.core.type.TypeReference<>() {});
            } catch (com.fasterxml.jackson.core.JsonProcessingException ignored) {
                // A malformed legacy value falls back to the safe per-kind default.
            }
        }
        java.util.Map<String, Object> config = new java.util.LinkedHashMap<>();
        String kind = widget.getQueryKind().name();
        if ("count".equals(kind) || "cctv".equals(kind)) {
            config.put("action", "none");
            return config;
        }
        config.put("action", "chart");
        config.put("chartStyle", "aggregate".equals(kind) ? "bar" : "line");
        String range = "aggregate".equals(kind) && widget.getAggregateRangePreset() != null
                ? widget.getAggregateRangePreset().name()
                : widget.getTrendRangePreset();
        config.put("rangePreset", range == null ? "last_3d" : range);
        config.put("window", widget.getTrendWindow() == null ? "15m" : widget.getTrendWindow());
        config.put("seriesMode", widget.getChartSeriesMode() == null ? "per_device" : widget.getChartSeriesMode().name());
        config.put("pointNames", widget.pointNames() == null ? java.util.List.of() : widget.pointNames());
        return config;
    }
}
