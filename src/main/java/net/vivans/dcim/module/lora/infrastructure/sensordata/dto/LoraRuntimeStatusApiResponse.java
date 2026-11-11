package net.vivans.dcim.module.lora.infrastructure.sensordata.dto;

import java.util.List;

/** Collector(MQTT 수신) ApiResponse<List<LoraDeviceRuntimeStatusResponse>> 역직렬화용 최소 래퍼. */
public record LoraRuntimeStatusApiResponse(
        int status,
        List<LoraDeviceRuntimeStatusResponse> data
) {
}
