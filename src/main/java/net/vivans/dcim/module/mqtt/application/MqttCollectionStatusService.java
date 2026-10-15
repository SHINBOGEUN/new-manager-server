package net.vivans.dcim.module.mqtt.application;
import net.vivans.dcim.module.mqtt.application.MqttSourceService;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.mqtt.api.dto.MqttCollectionSourceSummary;
import net.vivans.dcim.module.mqtt.api.dto.MqttCollectionStatusResponse;
import net.vivans.dcim.module.mqtt.api.dto.MqttCollectionStatusRow;
import net.vivans.dcim.module.mqtt.api.dto.MqttCollectionStatusSummary;
import net.vivans.dcim.module.mqtt.api.dto.MqttSourceResponse;
import net.vivans.dcim.module.mqtt.domain.model.DeviceMqttIdentity;
import net.vivans.dcim.module.mqtt.domain.model.MqttSourceStatusType;
import net.vivans.dcim.module.mqtt.domain.repository.DeviceMqttIdentityRepository;
import net.vivans.dcim.module.mqtt.domain.repository.DeviceModelMqttPointRepository;
import net.vivans.dcim.module.mqtt.infrastructure.influx.MqttCollectionInfluxQuery;
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
 * 공통 MQTT 수집 상태. 저장 확인은 최근 7일 Influx 이력 기준이며 현재 발행 보장은 아니다.
 * 소스는 형식별 집계이며 장비와 특정 브로커의 직접 연결을 추정하지 않는다.
 * Sensor Data 런타임 오류 API는 아직 LoRa만 지원하므로 AGENT의 오류 여부를 단정하지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MqttCollectionStatusService {

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

    private final DeviceMqttIdentityRepository mqttIdentityRepository;
    private final DeviceModelMqttPointRepository mqttPointRepository;
    private final MqttSourceService loraMqttSourceService;
    private final MqttCollectionInfluxQuery influxQuery;
    private final LoraRuntimeStatusClient runtimeStatusClient;

    public MqttCollectionStatusResponse getCollectionStatus() {
        List<MqttSourceResponse> sources = loraMqttSourceService.getAll();
        MqttCollectionSourceSummary sourceSummary = buildSourceSummary(sources);

        // 한 장비에 식별자가 여러 개면 활성 식별자를 대표로 선택한다.
        List<DeviceMqttIdentity> endpoints = mqttIdentityRepository.findAllOrderByIdAsc().stream()
                .collect(Collectors.toMap(e -> e.getDevice().getId(), e -> e,
                        (a, b) -> !a.isEnabled() && b.isEnabled() ? b : a, java.util.LinkedHashMap::new))
                .values().stream().toList();

        Set<Integer> deviceModelIds = endpoints.stream()
                .map(endpoint -> endpoint.getDevice().getDeviceModel().getId())
                .collect(Collectors.toSet());
        Set<Integer> modelIdsWithMapping = deviceModelIds.isEmpty() ? Set.of()
                : mqttPointRepository.findAllEnabledByDeviceModelIdIn(deviceModelIds).stream()
                        .map(point -> point.getDeviceModel().getId())
                        .collect(Collectors.toSet());

        List<Integer> deviceIds = endpoints.stream().map(endpoint -> endpoint.getDevice().getId()).distinct().toList();
        boolean influxAvailable = influxQuery.isAvailable();
        Map<Integer, List<LoraInfluxPointRecord>> influxByDevice;
        try { influxByDevice = influxQuery.findRecentMqttPoints(deviceIds); }
        catch (RuntimeException error) { influxByDevice = Map.of(); influxAvailable = false; }

        Map<Integer, LoraDeviceRuntimeStatusResponse> runtimeByDevice = runtimeStatusClient.getAll().stream()
                .filter(status -> status.deviceId() != null)
                .collect(Collectors.toMap(LoraDeviceRuntimeStatusResponse::deviceId, status -> status, (first, second) -> first));

        List<MqttCollectionStatusRow> rows = new ArrayList<>();
        int savedCount = 0;
        for (DeviceMqttIdentity endpoint : endpoints) {
            Integer deviceId = endpoint.getDevice().getId();
            Integer deviceModelId = endpoint.getDevice().getDeviceModel().getId();
            List<LoraInfluxPointRecord> points = influxByDevice.getOrDefault(deviceId, List.of());
            Instant lastSavedAt = points.stream().map(LoraInfluxPointRecord::time).max(Comparator.naturalOrder()).orElse(null);
            Integer lastPointCount = lastSavedAt == null ? null
                    : (int) points.stream().filter(point -> lastSavedAt.equals(point.time())).count();
            boolean lora = endpoint.getIdentityType().isLora();
            LoraDeviceRuntimeStatusResponse runtime = lora ? runtimeByDevice.get(deviceId) : null;
            var expectedType = lora
                    ? net.vivans.dcim.module.mqtt.domain.model.MqttSourceType.CHIRPSTACK_LORA
                    : net.vivans.dcim.module.mqtt.domain.model.MqttSourceType.AGENT;
            List<MqttSourceResponse> relevantSources = sources.stream()
                    .filter(source -> source.sourceType() == expectedType && source.enabled()).toList();
            boolean anyEnabledSource = !relevantSources.isEmpty();
            boolean anySourceConnected = relevantSources.stream()
                    .anyMatch(source -> source.status() != null && source.status().status() == MqttSourceStatusType.CONNECTED);

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
            } else if (!influxAvailable) {
                status = "INFLUX_UNAVAILABLE";
                statusMessage = "InfluxDB 조회 불가 — 저장 여부 확인 불가";
            } else if (lastSavedAt == null) {
                status = STATUS_NEVER_SAVED;
                statusMessage = "최근 7일 저장 이력 없음";
            } else {
                status = STATUS_SAVED;
                statusMessage = "저장 확인";
                savedCount++;
            }

            String recentErrorReason = runtime == null || runtime.lastErrorCode() == null ? null : formatErrorReason(runtime);
            Instant recentErrorAt = runtime == null || runtime.lastErrorCode() == null ? null : runtime.lastErrorAt();

            rows.add(new MqttCollectionStatusRow(
                    endpoint.getId(),
                    deviceId,
                    endpoint.getDevice().getName(),
                    deviceModelId,
                    endpoint.getDevice().getDeviceModel().getName(),
                    endpoint.getIdentityType(),
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

        MqttCollectionStatusSummary summary = new MqttCollectionStatusSummary(
                rows.size(), savedCount, rows.size() - savedCount, influxAvailable, Instant.now());
        return new MqttCollectionStatusResponse(summary, sourceSummary, rows);
    }

    private static MqttCollectionSourceSummary buildSourceSummary(List<MqttSourceResponse> sources) {
        int total = sources.size();
        int enabled = (int) sources.stream().filter(MqttSourceResponse::enabled).count();
        int connected = (int) sources.stream()
                .filter(source -> source.enabled() && source.status() != null && source.status().status() == MqttSourceStatusType.CONNECTED)
                .count();
        return MqttCollectionSourceSummary.of(total, enabled, connected);
    }

    /** runtime의 lastErrorCode(짧은 코드)에 lastErrorMessage(상세, 있으면)를 덧붙여 화면에 표시할 사유 문자열을 만든다. */
    private static String formatErrorReason(LoraDeviceRuntimeStatusResponse runtime) {
        String message = runtime.lastErrorMessage();
        return message == null || message.isBlank() ? runtime.lastErrorCode() : runtime.lastErrorCode() + " - " + message;
    }
}
