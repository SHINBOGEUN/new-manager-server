package net.vivans.dcim.module.lora.application;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.lora.api.dto.LoraMqttSourceRequest;
import net.vivans.dcim.module.lora.api.dto.LoraMqttSourceResponse;
import net.vivans.dcim.module.lora.api.dto.LoraMqttSourceStatusReportRequest;
import net.vivans.dcim.module.lora.api.dto.LoraMqttSourceStatusResponse;
import net.vivans.dcim.module.lora.domain.model.LoraMqttSource;
import net.vivans.dcim.module.lora.domain.model.LoraMqttSourceStatus;
import net.vivans.dcim.module.lora.domain.model.LoraMqttSourceType;
import net.vivans.dcim.module.lora.domain.repository.LoraMqttSourceRepository;
import net.vivans.dcim.module.lora.domain.repository.LoraMqttSourceStatusRepository;
import net.vivans.dcim.module.lora.infrastructure.sensordata.LoraMqttSourceSyncClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoraMqttSourceService {
    private final LoraMqttSourceRepository sourceRepository;
    private final LoraMqttSourceStatusRepository statusRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final LoraMqttSourceSyncClient syncClient;

    public List<LoraMqttSourceResponse> getAll() {
        List<LoraMqttSource> sources = sourceRepository.findAllOrderByIdAsc();
        if (sources.isEmpty()) {
            return List.of();
        }
        Map<Integer, LoraMqttSourceStatus> statuses = new HashMap<>();
        statusRepository.findAllBySourceIdIn(sources.stream().map(LoraMqttSource::getId).toList())
                .forEach(status -> statuses.put(status.getSourceId(), status));
        return sources.stream().map(source -> LoraMqttSourceResponse.from(source,
                LoraMqttSourceStatusResponse.from(statuses.get(source.getId())))).toList();
    }

    /**
     * Sensor Data가 시작/1분 재조정 시 쓰는 소스 설정 목록.
     * 비활성 소스도 함께 내려야 이미 연결된 client를 끊고 DISABLED 상태로 전환할 수 있다.
     */
    public List<LoraMqttSourceResponse> getAllForSensorData() {
        return sourceRepository.findAllOrderByIdAsc().stream()
                .map(source -> LoraMqttSourceResponse.from(source, LoraMqttSourceStatusResponse.from(null))).toList();
    }

    @Transactional
    public LoraMqttSourceResponse create(LoraMqttSourceRequest request) {
        validateName(request.name(), null);
        LoraMqttSource source = sourceRepository.save(LoraMqttSource.create(
                request.name(), defaultType(request.sourceType()), request.brokerUrl(), request.topic(),
                request.clientId(), request.credentialKey(), request.enabled() == null || request.enabled()));
        statusRepository.save(LoraMqttSourceStatus.initial(source.getId()));
        eventPublisher.publishEvent(new LoraMqttSourceChangedEvent("created:" + source.getId()));
        return LoraMqttSourceResponse.from(source, LoraMqttSourceStatusResponse.from(null));
    }

    @Transactional
    public LoraMqttSourceResponse update(Integer id, LoraMqttSourceRequest request) {
        LoraMqttSource source = findSource(id);
        validateName(request.name(), id);
        source.update(request.name(), defaultType(request.sourceType()), request.brokerUrl(), request.topic(),
                request.clientId(), request.credentialKey(), request.enabled() == null || request.enabled());
        LoraMqttSource saved = sourceRepository.save(source);
        eventPublisher.publishEvent(new LoraMqttSourceChangedEvent("updated:" + id));
        return LoraMqttSourceResponse.from(saved, LoraMqttSourceStatusResponse.from(statusRepository.findBySourceId(id).orElse(null)));
    }

    @Transactional
    public Integer delete(Integer id) {
        sourceRepository.delete(findSource(id));
        eventPublisher.publishEvent(new LoraMqttSourceChangedEvent("deleted:" + id));
        return id;
    }

    /** 운영 화면의 즉시 동기화 버튼용. DB 변경 없이 Sensor Data에 refresh 요청만 보낸다. */
    public void requestImmediateSync() {
        syncClient.requestRefresh("manual");
    }

    @Transactional
    public LoraMqttSourceStatusResponse reportStatus(Integer sourceId, LoraMqttSourceStatusReportRequest request) {
        findSource(sourceId);
        LoraMqttSourceStatus status = statusRepository.findBySourceId(sourceId)
                .orElseGet(() -> LoraMqttSourceStatus.initial(sourceId));
        status.report(request.status(), request.lastConnectedAt(), request.lastMessageAt(),
                request.messageCount() == null ? 0 : request.messageCount(),
                request.errorCount() == null ? 0 : request.errorCount(),
                request.reconnectCount() == null ? 0 : request.reconnectCount(), request.lastError(), request.reportedAt());
        return LoraMqttSourceStatusResponse.from(statusRepository.save(status));
    }

    private void validateName(String name, Integer excludeId) {
        boolean exists = excludeId == null ? sourceRepository.existsByNameIgnoreCase(name)
                : sourceRepository.existsByNameIgnoreCaseAndIdNot(name, excludeId);
        if (exists) throw new IllegalArgumentException("lora mqtt source name already exists");
    }

    private LoraMqttSource findSource(Integer id) {
        return sourceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("LoraMqttSource not found: " + id));
    }

    private LoraMqttSourceType defaultType(LoraMqttSourceType sourceType) {
        return sourceType == null ? LoraMqttSourceType.CHIRPSTACK_LORA : sourceType;
    }
}
