package net.vivans.dcim.module.lora.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.lora.domain.model.LoraIngestErrorLog;
import net.vivans.dcim.module.lora.domain.repository.LoraIngestErrorLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LoraIngestErrorLogJpaRepository implements LoraIngestErrorLogRepository {

    private final LoraIngestErrorLogSpringDataRepository springDataRepository;

    @Override
    public LoraIngestErrorLog save(LoraIngestErrorLog log) {
        return springDataRepository.save(log);
    }

    @Override
    public Optional<LoraIngestErrorLog> findById(Long id) {
        return springDataRepository.findById(id);
    }

    @Override
    public Page<LoraIngestErrorLog> findAllByResolved(Boolean resolved, Pageable pageable) {
        if (resolved == null) {
            return springDataRepository.findAllByOrderByReceivedAtDesc(pageable);
        }
        return springDataRepository.findAllByResolvedOrderByReceivedAtDesc(resolved, pageable);
    }

    @Override
    public long deleteByReceivedAtBefore(Instant threshold) {
        return springDataRepository.deleteByReceivedAtBefore(threshold);
    }
}
