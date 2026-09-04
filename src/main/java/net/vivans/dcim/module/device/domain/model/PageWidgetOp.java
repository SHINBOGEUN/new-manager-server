package net.vivans.dcim.module.device.domain.model;

public enum PageWidgetOp {
    usage,
    power;

    public static PageWidgetOp from(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return valueOf(raw.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("op must be usage or power");
        }
    }
}
