package net.vivans.dcim.module.device.domain.model;

public enum PageWidgetDeviceRole {
    DEFAULT,
    TOTAL,
    IT;

    public static PageWidgetDeviceRole from(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT;
        }
        return switch (raw.trim().toLowerCase()) {
            case "default" -> DEFAULT;
            case "total" -> TOTAL;
            case "it" -> IT;
            default -> throw new IllegalArgumentException(
                    "deviceRole must be default, total, or it");
        };
    }

    /** DB / API wire value (lowercase). */
    public String wireValue() {
        return name().toLowerCase();
    }
}
