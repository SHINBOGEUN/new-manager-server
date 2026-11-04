package net.vivans.dcim.module.device.domain.model;

public enum PageWidgetOp {
    usage,
    power,
    usage_rank,
    power_rank,
    demand;

    public static PageWidgetOp from(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return valueOf(raw.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("unsupported aggregate op");
        }
    }
}
