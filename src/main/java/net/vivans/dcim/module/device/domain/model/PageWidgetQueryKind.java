package net.vivans.dcim.module.device.domain.model;

public enum PageWidgetQueryKind {
    last,
    aggregate,
    count;

    public static PageWidgetQueryKind from(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("queryKind is required");
        }
        try {
            return valueOf(raw.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("queryKind must be last, aggregate, or count");
        }
    }
}
