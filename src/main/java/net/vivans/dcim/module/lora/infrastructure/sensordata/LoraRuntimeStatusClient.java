package net.vivans.dcim.module.lora.infrastructure.sensordata;

import lombok.extern.slf4j.Slf4j;
import net.vivans.dcim.module.lora.infrastructure.sensordata.dto.LoraDeviceRuntimeStatusResponse;
import net.vivans.dcim.module.lora.infrastructure.sensordata.dto.LoraRuntimeStatusApiResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Manager가 "LoRa 수집 상태" 화면을 조합할 때 Sensor Data의 장비별 런타임 상태(메모리)를 조회한다.
 * Sensor Data가 꺼져 있거나 응답이 없어도 오류로 취급하지 않고 빈 목록을 반환한다 — 이 경우 Manager는
 * InfluxDB 저장 이력과 설정 정보만으로 상태를 계산한다(요청 기준: "Sensor Data runtime status가 없으면
 * 오류로 처리하지 않는다").
 */
@Slf4j
@Component
public class LoraRuntimeStatusClient {

    private final RestClient restClient;
    private final SensorDataServiceProperties properties;

    public LoraRuntimeStatusClient(@Qualifier("sensorDataRestClient") RestClient restClient,
                                   SensorDataServiceProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public List<LoraDeviceRuntimeStatusResponse> getAll() {
        if (!properties.isEnabled()) {
            return List.of();
        }
        try {
            LoraRuntimeStatusApiResponse response = restClient.get()
                    .uri("/api/internal/lora/runtime-status")
                    .retrieve()
                    .body(LoraRuntimeStatusApiResponse.class);
            return response == null || response.data() == null ? List.of() : response.data();
        } catch (Exception exception) {
            log.warn("[LORA_RUNTIME_STATUS_FETCH_FAILED] exception={} message={}",
                    exception.getClass().getSimpleName(), exception.getMessage());
            return List.of();
        }
    }
}
