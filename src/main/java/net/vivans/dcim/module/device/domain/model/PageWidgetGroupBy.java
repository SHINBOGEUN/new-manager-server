package net.vivans.dcim.module.device.domain.model;

public enum PageWidgetGroupBy {
    device,
    point,
    location;

    public static PageWidgetGroupBy from(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return valueOf(raw.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("groupBy must be device, point, or location");
        }
    }
}
