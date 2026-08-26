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
        List<Integer> deviceIds,
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
                widget.getWeightPoint(),
                widget.getNumeratorPoint(),
                widget.getDenominatorPoint(),
                widget.deviceIds(),
                widget.pointNames(),
                PageWidgetLayoutResponse.from(widget.getLayout())
        );
    }
}
