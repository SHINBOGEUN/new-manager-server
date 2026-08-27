package net.vivans.dcim.module.query.api.dto;

import java.util.List;

public record CountWidgetResponse(
        Integer widgetId,
        String widgetName,
        String pageCode,
        String countMode,
        Integer countModelId,
        int count,
        List<CountByModelResponse> byModel
) {
}
