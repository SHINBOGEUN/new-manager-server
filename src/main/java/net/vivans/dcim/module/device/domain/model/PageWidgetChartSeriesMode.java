package net.vivans.dcim.module.device.domain.model;

public enum PageWidgetChartSeriesMode {
    per_device,
    sum,
    by_phase,
    by_path;

    public static PageWidgetChartSeriesMode from(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return valueOf(raw.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "chartSeriesMode must be per_device, sum, by_phase, or by_path");
        }
    }
}
