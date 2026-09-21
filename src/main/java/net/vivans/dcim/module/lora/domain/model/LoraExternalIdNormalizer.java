package net.vivans.dcim.module.lora.domain.model;

/**
 * 외부 식별자(devEUI, deviceName 등)를 저장·조회 기준으로 정규화한다.
 * 대문자로 통일하고 ':', '-', '_', 공백 같은 구분자를 제거한다.
 * 등록 시점과 수신 payload 조회 시점 양쪽에서 반드시 같은 규칙을 사용해야 한다.
 */
public final class LoraExternalIdNormalizer {

    private LoraExternalIdNormalizer() {
    }

    public static String normalize(String externalId) {
        if (externalId == null) {
            return null;
        }
        return externalId.trim()
                .toUpperCase()
                .replace(":", "")
                .replace("-", "")
                .replace("_", "")
                .replace(" ", "");
    }
}
