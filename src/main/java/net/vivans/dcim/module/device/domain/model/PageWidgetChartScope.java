package net.vivans.dcim.module.device.domain.model;

public enum PageWidgetChartScope {
    devices,
    models;

    public static PageWidgetChartScope from(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return valueOf(raw.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("chartScope must be devices or models");
        }
    }
}
