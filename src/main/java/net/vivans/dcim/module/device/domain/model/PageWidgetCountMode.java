package net.vivans.dcim.module.device.domain.model;

public enum PageWidgetCountMode {
    total,
    by_model,
    model;

    public static PageWidgetCountMode from(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return valueOf(raw.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("countMode must be total, by_model, or model");
        }
    }
}
