package net.vivans.dcim.module.lora.application;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import net.vivans.dcim.module.lora.api.dto.LoraIngestErrorLogCreateRequest;
import net.vivans.dcim.module.lora.api.dto.LoraIngestErrorLogResponse;
import net.vivans.dcim.module.lora.domain.model.LoraIngestErrorLog;
import net.vivans.dcim.module.lora.domain.repository.LoraIngestErrorLogRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * LoRa 수집 오류 이력 기록·조회·정리.
 * 보관 기간은 lora.error-log.retention-days (기본 14일)로 설정하며, 매일 자정 스케줄로 정리한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoraIngestErrorLogService {

    private final LoraIngestErrorLogRepository loraIngestErrorLogRepository;
    private final DeviceRepository deviceRepository;

    @Value("${lora.error-log.retention-days:14}")
    private int retentionDays;

    public Page<LoraIngestErrorLogResponse> getAll(Boolean resolved, Pageable pageable) {
        return loraIngestErrorLogRepository.findAllByResolved(resolved, pageable)
                .map(LoraIngestErrorLogResponse::from);
    }

    @Transactional
    public LoraIngestErrorLogResponse create(LoraIngestErrorLogCreateRequest request) {
        Device device = request.deviceId() == null ? null : deviceRepository.findById(request.deviceId()).orElse(null);
        LoraIngestErrorLog saved = loraIngestErrorLogRepository.save(LoraIngestErrorLog.create(
                request.receivedAt(), device, request.externalId(), request.idType(),
                request.reason(), request.rawPayload()));
        return LoraIngestErrorLogResponse.from(saved);
    }

    @Transactional
    public LoraIngestErrorLogResponse resolve(Long id, boolean resolved) {
        LoraIngestErrorLog log = loraIngestErrorLogRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("LoraIngestErrorLog not found: " + id));
        log.markResolved(resolved);
        return LoraIngestErrorLogResponse.from(log);
    }

    /** 매일 03:00, 보관 기간이 지난 오류 로그를 정리한다. */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupExpired() {
        Instant threshold = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        long deleted = loraIngestErrorLogRepository.deleteByReceivedAtBefore(threshold);
        if (deleted > 0) {
            log.info("lora_ingest_error_log cleanup: deleted={} thresholdBefore={}", deleted, threshold);
        }
    }
}
