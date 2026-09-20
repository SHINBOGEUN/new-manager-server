package net.vivans.dcim.module.lora.domain.repository;

import net.vivans.dcim.module.lora.domain.model.LoraIngestErrorLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Optional;

public interface LoraIngestErrorLogRepository {

    LoraIngestErrorLog save(LoraIngestErrorLog log);

    Optional<LoraIngestErrorLog> findById(Long id);

    Page<LoraIngestErrorLog> findAllByResolved(Boolean resolved, Pageable pageable);

    long deleteByReceivedAtBefore(Instant threshold);
}
