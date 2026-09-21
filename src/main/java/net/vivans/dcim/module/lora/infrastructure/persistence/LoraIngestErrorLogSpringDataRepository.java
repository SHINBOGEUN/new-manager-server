package net.vivans.dcim.module.lora.infrastructure.persistence;

import net.vivans.dcim.module.lora.domain.model.LoraIngestErrorLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface LoraIngestErrorLogSpringDataRepository extends JpaRepository<LoraIngestErrorLog, Long> {

    @EntityGraph(attributePaths = {"device"})
    Page<LoraIngestErrorLog> findAllByResolvedOrderByReceivedAtDesc(boolean resolved, Pageable pageable);

    @EntityGraph(attributePaths = {"device"})
    Page<LoraIngestErrorLog> findAllByOrderByReceivedAtDesc(Pageable pageable);

    long deleteByReceivedAtBefore(Instant threshold);
}
