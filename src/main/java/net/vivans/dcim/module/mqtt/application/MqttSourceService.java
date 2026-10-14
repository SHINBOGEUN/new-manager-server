package net.vivans.dcim.module.mqtt.application;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.mqtt.api.dto.MqttSourceRequest;
import net.vivans.dcim.module.mqtt.api.dto.MqttSourceResponse;
import net.vivans.dcim.module.mqtt.api.dto.MqttSourceStatusReportRequest;
import net.vivans.dcim.module.mqtt.api.dto.MqttSourceStatusResponse;
import net.vivans.dcim.module.mqtt.domain.model.MqttSource;
import net.vivans.dcim.module.mqtt.domain.model.MqttSourceStatus;
import net.vivans.dcim.module.mqtt.domain.model.MqttSourceType;
import net.vivans.dcim.module.mqtt.domain.repository.MqttSourceRepository;
import net.vivans.dcim.module.mqtt.domain.repository.MqttSourceStatusRepository;
import net.vivans.dcim.module.mqtt.infrastructure.sensordata.MqttSourceSyncClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MqttSourceService {
    private final MqttSourceRepository sourceRepository;
    private final MqttSourceStatusRepository statusRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final MqttSourceSyncClient syncClient;

    public List<MqttSourceResponse> getAll() {
        List<MqttSource> sources = sourceRepository.findAllOrderByIdAsc();
        if (sources.isEmpty()) {
            return List.of();
        }
        Map<Integer, MqttSourceStatus> statuses = new HashMap<>();
        statusRepository.findAllBySourceIdIn(sources.stream().map(MqttSource::getId).toList())
                .forEach(status -> statuses.put(status.getSourceId(), status));
        return sources.stream().map(source -> MqttSourceResponse.from(source,
                MqttSourceStatusResponse.from(statuses.get(source.getId())))).toList();
    }

    /**
     * Sensor Data가 시작/1분 재조정 시 쓰는 소스 설정 목록.
     * 비활성 소스도 함께 내려야 이미 연결된 client를 끊고 DISABLED 상태로 전환할 수 있다.
     */
    public List<MqttSourceResponse> getAllForSensorData() {
        return sourceRepository.findAllOrderByIdAsc().stream()
                .map(source -> MqttSourceResponse.from(source, MqttSourceStatusResponse.from(null))).toList();
    }

    @Transactional
    public MqttSourceResponse create(MqttSourceRequest request) {
        validateName(request.name(), null);
        validateClientId(request, null);
        MqttSource source = sourceRepository.save(MqttSource.create(
                request.name(), defaultType(request.sourceType()), request.brokerUrl(), request.topic(),
                request.clientId(), request.credentialKey(), request.enabled() == null || request.enabled()));
        statusRepository.save(MqttSourceStatus.initial(source.getId()));
        eventPublisher.publishEvent(new MqttSourceChangedEvent("created:" + source.getId()));
        return MqttSourceResponse.from(source, MqttSourceStatusResponse.from(null));
    }

    @Transactional
    public MqttSourceResponse update(Integer id, MqttSourceRequest request) {
        MqttSource source = findSource(id);
        validateName(request.name(), id);
        validateClientId(request, id);
        source.update(request.name(), request.sourceType() == null ? source.getSourceType() : request.sourceType(), request.brokerUrl(), request.topic(),
                request.clientId(), request.credentialKey(), request.enabled() == null || request.enabled());
        MqttSource saved = sourceRepository.save(source);
        eventPublisher.publishEvent(new MqttSourceChangedEvent("updated:" + id));
        return MqttSourceResponse.from(saved, MqttSourceStatusResponse.from(statusRepository.findBySourceId(id).orElse(null)));
    }

    @Transactional
    public Integer delete(Integer id) {
        MqttSource source = findSource(id);
        statusRepository.findBySourceId(id).ifPresent(statusRepository::delete);
        sourceRepository.delete(source);
        eventPublisher.publishEvent(new MqttSourceChangedEvent("deleted:" + id));
        return id;
    }

    /** 운영 화면의 즉시 동기화 버튼용. DB 변경 없이 Sensor Data에 refresh 요청만 보낸다. */
    public void requestImmediateSync() {
        syncClient.requestRefresh("manual");
    }

    @Transactional
    public MqttSourceStatusResponse reportStatus(Integer sourceId, MqttSourceStatusReportRequest request) {
        findSource(sourceId);
        MqttSourceStatus status = statusRepository.findBySourceId(sourceId)
                .orElseGet(() -> MqttSourceStatus.initial(sourceId));
        status.report(request.status(), request.lastConnectedAt(), request.lastMessageAt(),
                request.messageCount() == null ? 0 : request.messageCount(),
                request.errorCount() == null ? 0 : request.errorCount(),
                request.reconnectCount() == null ? 0 : request.reconnectCount(), request.lastError(), request.reportedAt());
        return MqttSourceStatusResponse.from(statusRepository.save(status));
    }

    private void validateName(String name, Integer excludeId) {
        boolean exists = excludeId == null ? sourceRepository.existsByNameIgnoreCase(name)
                : sourceRepository.existsByNameIgnoreCaseAndIdNot(name, excludeId);
        if (exists) throw new IllegalArgumentException("mqtt source name already exists");
    }

    private void validateClientId(MqttSourceRequest request, Integer excludeId) {
        if (request.clientId() == null || request.clientId().isBlank() || request.brokerUrl() == null) return;
        boolean duplicate = sourceRepository.findAllOrderByIdAsc().stream()
                .filter(source -> !java.util.Objects.equals(source.getId(), excludeId))
                .anyMatch(source -> request.brokerUrl().trim().equals(source.getBrokerUrl())
                        && request.clientId().trim().equals(source.getClientId()));
        if (duplicate) throw new IllegalArgumentException("MQTT client ID is already used on this broker");
    }

    private MqttSource findSource(Integer id) {
        return sourceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("MqttSource not found: " + id));
    }

    private MqttSourceType defaultType(MqttSourceType sourceType) {
        return sourceType == null ? MqttSourceType.CHIRPSTACK_LORA : sourceType;
    }

    public void requireLoraSource(Integer id) {
        if (findSource(id).getSourceType() != MqttSourceType.CHIRPSTACK_LORA) {
            throw new IllegalArgumentException("Legacy LoRa API cannot access an AGENT source");
        }
    }
}
