package net.vivans.dcim.module.lora.api.dto;

/**
 * LoRa MQTT 수집 소스 전체 연결 상태 요약.
 * 장비-소스 간 명시적 연결 구조가 없어(메시지 내용으로만 장비를 식별) 소스 단위는 집계로만 표현한다.
 */
public record LoraCollectionSourceSummary(
        int totalSourceCount,
        int enabledSourceCount,
        int connectedSourceCount,
        String overallStatus
) {
    public static final String STATUS_NOT_CONFIGURED = "NOT_CONFIGURED";
    public static final String STATUS_DOWN = "DOWN";
    public static final String STATUS_PARTIAL = "PARTIAL";
    public static final String STATUS_CONNECTED = "CONNECTED";

    public static LoraCollectionSourceSummary of(int totalSourceCount, int enabledSourceCount, int connectedSourceCount) {
        String overallStatus;
        if (totalSourceCount == 0) {
            overallStatus = STATUS_NOT_CONFIGURED;
        } else if (connectedSourceCount == 0) {
            overallStatus = STATUS_DOWN;
        } else if (enabledSourceCount > 0 && connectedSourceCount < enabledSourceCount) {
            overallStatus = STATUS_PARTIAL;
        } else {
            overallStatus = STATUS_CONNECTED;
        }
        return new LoraCollectionSourceSummary(totalSourceCount, enabledSourceCount, connectedSourceCount, overallStatus);
    }
}
