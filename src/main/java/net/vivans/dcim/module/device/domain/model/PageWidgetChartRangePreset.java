package net.vivans.dcim.module.device.domain.model;

public enum PageWidgetChartRangePreset {
    last_24h,
    today,
    yesterday,
    last_7d,
    this_month,
    last_month;

    public static PageWidgetChartRangePreset from(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return valueOf(raw.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "chartRangePreset must be last_24h, today, yesterday, last_7d, this_month, or last_month");
        }
    }
}
