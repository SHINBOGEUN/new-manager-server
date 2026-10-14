package net.vivans.dcim.module.mqtt.domain.model;

import java.util.Locale;

/** 저장과 수신 조회에 동일하게 적용하는 유형별 식별자 정규화. */
public final class MqttExternalIdNormalizer {
    private MqttExternalIdNormalizer() {}

    public static String normalize(MqttIdentityType type, String value) {
        if (type == null) {
            throw new IllegalArgumentException("identityType is required");
        }
        if (value == null || value.isBlank() || value.trim().length() > 255) {
            throw new IllegalArgumentException("externalId must contain 1 to 255 characters");
        }
        String trimmed = value.trim();
        String normalized = switch (type) {
            // 기존 LoRa 정규화와 일치시켜 이관 후에도 매칭 결과를 보존한다.
            case MAC, DEV_EUI, DEVICE_NAME -> trimmed.toUpperCase(Locale.ROOT)
                    .replace(":", "").replace("-", "").replace("_", "").replace(" ", "");
            case UUID -> java.util.UUID.fromString(trimmed).toString();
            case SERIAL -> trimmed;
        };
        if (normalized.isBlank() || normalized.length() > 255) {
            throw new IllegalArgumentException("normalized externalId must contain 1 to 255 characters");
        }
        if (type == MqttIdentityType.MAC && !normalized.matches("[0-9A-F]{12}")) {
            throw new IllegalArgumentException("MAC must contain 12 hexadecimal digits");
        }
        return normalized;
    }
}
