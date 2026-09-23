package net.vivans.dcim.module.lora.application;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.lora.api.dto.LoraCollectionSourceSummary;
import net.vivans.dcim.module.lora.api.dto.LoraCollectionStatusResponse;
import net.vivans.dcim.module.lora.api.dto.LoraCollectionStatusRow;
import net.vivans.dcim.module.lora.api.dto.LoraCollectionStatusSummary;
import net.vivans.dcim.module.lora.api.dto.LoraMqttSourceResponse;
import net.vivans.dcim.module.lora.domain.model.DeviceLoraEndpoint;
import net.vivans.dcim.module.lora.domain.model.LoraMqttSourceStatusType;
import net.vivans.dcim.module.lora.domain.repository.DeviceLoraEndpointRepository;
import net.vivans.dcim.module.lora.domain.repository.DeviceModelLoraPointRepository;
import net.vivans.dcim.module.lora.infrastructure.influx.LoraCollectionInfluxQuery;
import net.vivans.dcim.module.lora.infrastructure.influx.LoraInfluxPointRecord;
import net.vivans.dcim.module.lora.infrastructure.sensordata.LoraRuntimeStatusClient;
import net.vivans.dcim.module.lora.infrastructure.sensordata.dto.LoraDeviceRuntimeStatusResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * LoRa 실제 수집 검증(1차) — Ops Console "LoRa 수집 상태" 탭이 사용하는 조회 전용 서비스.
 * 새 DB 테이블/컬럼 없이 기존 device_lora_endpoint / device_model_lora_point / lora_mqtt_source(_status) 와
 * InfluxDB(protocol=mqtt), 그리고 Sensor Data의 장비별 런타임 상태(메모리, GET /api/internal/lora/runtime-status)
 * 를 조합해서 판정한다. 영구 오류 이력 테이블(lora_ingest_error_log)은 더 이상 존재하지 않는다.
 *
 * 상태 판정 우선순위(위에서부터 순서대로 검사, 먼저 만족하는 조건이 최종 상태):
 *   1) DISABLED             — endpoint 비활성
 *   2) NO_MAPPING            — 장비 모델에 활성화된 필드 매핑 없음
 *   3) NO_SOURCE             — 활성(enabled) MQTT 수집 소스가 하나도 없음
 *   4) SOURCE_DOWN           — 활성 소스는 있으나 CONNECTED 상태인 소스가 없음
 *   5) INFLUX_WRITE_FAILED   — Sensor Data runtime cache의 최근 오류가 INFLUX_WRITE_FAILED (저장 이력 유무와 무관)
 *   6) FIELD_ERROR           — Sensor Data runtime cache의 최근 오류가 FIELD_CONVERSION_FAILED (저장 이력 유무와 무관)
 *   7) NEVER_SAVED           — 위 오류가 없고 InfluxDB(protocol=mqtt)에 저장 이력이 전혀 없음
 *   8) SAVED                 — 위 조건에 해당하지 않고 저장 이력이 있음(정상)
 *
 * INFLUX_WRITE_FAILED/FIELD_ERROR는 저장 이력(lastSavedAt) 유무보다 우선한다. Sensor Data runtime status가
 * 없으면(서비스 재시작 직후 등) 오류로 취급하지 않고 InfluxDB 저장 이력과 설정 정보만으로 상태를 계산한다.
 *
 * 미등록(devEUI/deviceName) 장비는 운영 정책상 Sensor Data가 DEBUG 로그만 남기고 조용히 무시하므로
 * 이 화면·API의 판정 대상이 아니다.
 * 1차 범위에서는 STALE(지연) 상태를 판정하지 않는다 — device_lora_endpoint/device_model_lora_point에
 * 장비별 전송 주기 정보가 없어 오탐 없이 판정할 기준이 없기 때문이며, 2차 확장 범위로 분리했다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoraCollectionStatusService {

    private static final String REASON_INFLUX_WRITE_FAILED = "INFLUX_WRITE_FAILED";
    private static final String REASON_FIELD_CONVERSION_FAILED = "FIELD_CONVERSION_FAILED";

    public static final String STATUS_DISABLED = "DISABLED";
    public static final String STATUS_NO_MAPPING = "NO_MAPPING";
    public static final String STATUS_NO_SOURCE = "NO_SOURCE";
    public static final String STATUS_SOURCE_DOWN = "SOURCE_DOWN";
    public static final String STATUS_NEVER_SAVED = "NEVER_SAVED";
    public static final String STATUS_INFLUX_WRITE_FAILED = "INFLUX_WRITE_FAILED";
    public static final String STATUS_FIELD_ERROR = "FIELD_ERROR";
    public static final String STATUS_SAVED = "SAVED";

    private final DeviceLoraEndpointRepository deviceLoraEndpointRepository;
    private final DeviceModelLoraPointRepository deviceModelLoraPointRepository;
    private final LoraMqttSourceService loraMqttSourceService;
    private final LoraCollectionInfluxQuery influxQuery;
    private final LoraRuntimeStatusClient runtimeStatusClient;

    public LoraCollectionStatusResponse getCollectionStatus() {
        List<LoraMqttSourceResponse> sources = loraMqttSourceService.getAll();
        LoraCollectionSourceSummary sourceSummary = buildSourceSummary(sources);

        List<DeviceLoraEndpoint> endpoints = deviceLoraEndpointRepository.findAllOrderByIdAsc();

        Set<Integer> deviceModelIds = endpoints.stream()
                .map(endpoint -> endpoint.getDevice().getDeviceModel().getId())
                .collect(Collectors.toSet());
        Set<Integer> modelIdsWithMapping = deviceModelIds.isEmpty() ? Set.of()
                : deviceModelLoraPointRepository.findAllEnabledByDeviceModelIdIn(deviceModelIds).stream()
                        .map(point -> point.getDeviceModel().getId())
                        .collect(Collectors.toSet());

        List<Integer> deviceIds = endpoints.stream().map(endpoint -> endpoint.getDevice().getId()).distinct().toList();
        Map<Integer, List<LoraInfluxPointRecord>> influxByDevice = influxQuery.findRecentMqttPoints(deviceIds);

        Map<Integer, LoraDeviceRuntimeStatusResponse> runtimeByDevice = runtimeStatusClient.getAll().stream()
                .filter(status -> status.deviceId() != null)
                .collect(Collectors.toMap(LoraDeviceRuntimeStatusResponse::deviceId, status -> status, (first, second) -> first));

        boolean anyEnabledSource = sourceSummary.enabledSourceCount() > 0;
        boolean anySourceConnected = sourceSummary.connectedSourceCount() > 0;

        List<LoraCollectionStatusRow> rows = new ArrayList<>();
        int savedCount = 0;
        for (DeviceLoraEndpoint endpoint : endpoints) {
            Integer deviceId = endpoint.getDevice().getId();
            Integer deviceModelId = endpoint.getDevice().getDeviceModel().getId();
            List<LoraInfluxPointRecord> points = influxByDevice.getOrDefault(deviceId, List.of());
            Instant lastSavedAt = points.stream().map(LoraInfluxPointRecord::time).max(Comparator.naturalOrder()).orElse(null);
            Integer lastPointCount = lastSavedAt == null ? null
                    : (int) points.stream().filter(point -> lastSavedAt.equals(point.time())).count();
            LoraDeviceRuntimeStatusResponse runtime = runtimeByDevice.get(deviceId);

            String status;
            String statusMessage;
            if (!endpoint.isEnabled()) {
                status = STATUS_DISABLED;
                statusMessage = "엔드포인트 비활성";
            } else if (!modelIdsWithMapping.contains(deviceModelId)) {
                status = STATUS_NO_MAPPING;
                statusMessage = "모델 매핑 없음";
            } else if (!anyEnabledSource) {
                status = STATUS_NO_SOURCE;
                statusMessage = "MQTT 수집 소스 미등록";
            } else if (!anySourceConnected) {
                status = STATUS_SOURCE_DOWN;
                statusMessage = "MQTT 수집 소스 연결 끊김";
            } else if (runtime != null && REASON_INFLUX_WRITE_FAILED.equals(runtime.lastErrorCode())) {
                status = STATUS_INFLUX_WRITE_FAILED;
                statusMessage = "InfluxDB 저장 실패";
            } else if (runtime != null && REASON_FIELD_CONVERSION_FAILED.equals(runtime.lastErrorCode())) {
                status = STATUS_FIELD_ERROR;
                statusMessage = "일부 필드 변환 실패";
            } else if (lastSavedAt == null) {
                status = STATUS_NEVER_SAVED;
                statusMessage = "아직 저장 이력 없음";
            } else {
                status = STATUS_SAVED;
                statusMessage = "저장 확인";
                savedCount++;
            }

            String recentErrorReason = runtime == null || runtime.lastErrorCode() == null ? null : formatErrorReason(runtime);
            Instant recentErrorAt = runtime == null || runtime.lastErrorCode() == null ? null : runtime.lastErrorAt();

            rows.add(new LoraCollectionStatusRow(
                    endpoint.getId(),
                    deviceId,
                    endpoint.getDevice().getName(),
                    deviceModelId,
                    endpoint.getDevice().getDeviceModel().getName(),
                    endpoint.getIdType(),
                    endpoint.getExternalId(),
                    endpoint.isEnabled(),
                    status,
                    statusMessage,
                    lastSavedAt,
                    lastPointCount,
                    recentErrorReason,
                    recentErrorAt
            ));
        }

        LoraCollectionStatusSummary summary = new LoraCollectionStatusSummary(
                rows.size(), savedCount, rows.size() - savedCount, influxQuery.isAvailable(), Instant.now());
        return new LoraCollectionStatusResponse(summary, sourceSummary, rows);
    }

    private static LoraCollectionSourceSummary buildSourceSummary(List<LoraMqttSourceResponse> sources) {
        int total = sources.size();
        int enabled = (int) sources.stream().filter(LoraMqttSourceResponse::enabled).count();
        int connected = (int) sources.stream()
                .filter(source -> source.status() != null && source.status().status() == LoraMqttSourceStatusType.CONNECTED)
                .count();
        return LoraCollectionSourceSummary.of(total, enabled, connected);
    }

    /** runtime의 lastErrorCode(짧은 코드)에 lastErrorMessage(상세, 있으면)를 덧붙여 화면에 표시할 사유 문자열을 만든다. */
    private static String formatErrorReason(LoraDeviceRuntimeStatusResponse runtime) {
        String message = runtime.lastErrorMessage();
        return message == null || message.isBlank() ? runtime.lastErrorCode() : runtime.lastErrorCode() + " - " + message;
    }
}
