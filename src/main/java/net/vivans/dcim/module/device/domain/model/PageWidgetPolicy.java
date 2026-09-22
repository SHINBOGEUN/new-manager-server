package net.vivans.dcim.module.device.domain.model;

import net.vivans.dcim.module.common.domain.model.CommonCode;

final class PageWidgetPolicy {

    private PageWidgetPolicy() {
    }

    static void validateIdentity(CommonCode pageCode, String name, PageWidgetQueryKind queryKind) {
        if (pageCode == null) {
            throw new IllegalArgumentException("pageCode is required");
        }
        if (!DevicePageCodes.DEVICE_PAGE_GROUP_KEY.equals(pageCode.getCodeGroup().getGroupKey())) {
            throw new IllegalArgumentException("pageCode must belong to DEVICE_PAGE group");
        }
        validateName(name);
        if (queryKind == null) {
            throw new IllegalArgumentException("queryKind is required");
        }
    }

    static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
    }

    static void validateKindOptions(
            PageWidgetQueryKind queryKind,
            boolean aggregateConfigured,
            PageWidgetOp op,
            PageWidgetChartRangePreset aggregateRangePreset,
            PageWidgetCountMode countMode,
            Integer countModelId,
            PageWidgetChartScope chartScope,
            PageWidgetChartSeriesMode chartSeriesMode,
            PageWidgetChartRangePreset chartRangePreset,
            String chartWindow
    ) {
        if (queryKind == PageWidgetQueryKind.aggregate && !aggregateConfigured) {
            throw new IllegalArgumentException("aggregatePreset/op is required for aggregate");
        }
        if (queryKind != PageWidgetQueryKind.aggregate && op != null) {
            throw new IllegalArgumentException("op is only allowed for aggregate");
        }
        if (queryKind != PageWidgetQueryKind.aggregate && aggregateRangePreset != null) {
            throw new IllegalArgumentException("aggregateRangePreset is only allowed for aggregate");
        }
        if (queryKind != PageWidgetQueryKind.count && (countMode != null || countModelId != null)) {
            throw new IllegalArgumentException("countMode is only allowed for count");
        }
        if (queryKind != PageWidgetQueryKind.chart
                && (chartScope != null || chartSeriesMode != null
                || chartRangePreset != null || (chartWindow != null && !chartWindow.isBlank()))) {
            throw new IllegalArgumentException("chart options are only allowed for chart");
        }
    }

    static void validateBindings(
            PageWidgetQueryKind queryKind,
            PageWidgetChartScope chartScope,
            int pointCount,
            int deviceCount,
            int deviceGroupCount,
            int modelCount
    ) {
        if (queryKind == PageWidgetQueryKind.count || queryKind == PageWidgetQueryKind.pue
                || queryKind == PageWidgetQueryKind.psychrometric
                || queryKind == PageWidgetQueryKind.power_distribution) {
            return;
        }
        if (queryKind == PageWidgetQueryKind.chart) {
            if (pointCount == 0) {
                throw new IllegalArgumentException("pointNames is required for chart");
            }
            if (chartScope == PageWidgetChartScope.models) {
                if (modelCount == 0) {
                    throw new IllegalArgumentException("modelIds is required when chartScope is models");
                }
            } else if (deviceCount == 0 && deviceGroupCount == 0) {
                throw new IllegalArgumentException("deviceIds is required when chartScope is devices");
            }
            return;
        }
        if (queryKind == PageWidgetQueryKind.aggregate) {
            if (deviceCount == 0 && deviceGroupCount == 0) {
                throw new IllegalArgumentException("deviceIds is required");
            }
            if (pointCount == 0) {
                throw new IllegalArgumentException("pointNames is required for aggregate");
            }
            if (pointCount != 1) {
                throw new IllegalArgumentException("aggregate supports exactly one pointName");
            }
            return;
        }
        if (deviceCount == 0 && deviceGroupCount == 0) {
            throw new IllegalArgumentException("deviceIds is required");
        }
        if (queryKind == PageWidgetQueryKind.last && pointCount == 0) {
            throw new IllegalArgumentException("pointNames is required for last");
        }
    }
}
