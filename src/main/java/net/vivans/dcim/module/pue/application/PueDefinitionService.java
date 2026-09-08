package net.vivans.dcim.module.pue.application;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import net.vivans.dcim.module.device.domain.repository.PageWidgetRepository;
import net.vivans.dcim.module.pue.api.dto.*;
import net.vivans.dcim.module.pue.domain.model.*;
import net.vivans.dcim.module.pue.domain.repository.PueDefinitionRepository;
import net.vivans.dcim.shared.exception.ConflictException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PueDefinitionService {
    private final PueDefinitionRepository repository;
    private final DeviceRepository deviceRepository;
    private final PageWidgetRepository pageWidgetRepository;
    private final PueCollectorSyncService collectorSyncService;

    public List<PueDefinitionResponse> list() {
        return repository.findAll().stream().map(PueDefinitionResponse::from).toList();
    }

    @Transactional
    public PueDefinitionResponse create(PueDefinitionRequest request) {
        if (repository.existsByName(request.name().trim())) {
            throw new ConflictException("PUE definition name already exists");
        }
        PueDefinition definition = repository.save(PueDefinition.create(
                request.name(),
                request.calculationCron(),
                request.collectionEnabled() == null || request.collectionEnabled(),
                sources(request)
        ));
        collectorSyncService.sync(definition);
        log.info("[PUE_DEFINITION] action=CREATE definitionId={} name={} sourceCount={} enabled={} cron={}",
                definition.getId(), definition.getName(), definition.getSources().size(),
                definition.isCollectionEnabled(), definition.getCalculationCron());
        return PueDefinitionResponse.from(definition);
    }

    @Transactional
    public PueDefinitionResponse update(Integer id, PueDefinitionRequest request) {
        PueDefinition definition = find(id);
        if (repository.existsByNameAndIdNot(request.name().trim(), id)) {
            throw new ConflictException("PUE definition name already exists");
        }
        definition.update(request.name(), request.calculationCron(), sources(request));
        if (request.collectionEnabled() != null) {
            definition.setCollectionEnabled(request.collectionEnabled());
        }
        definition = repository.save(definition);
        collectorSyncService.sync(definition);
        log.info("[PUE_DEFINITION] action=UPDATE definitionId={} name={} sourceCount={} enabled={} cron={} version={}",
                definition.getId(), definition.getName(), definition.getSources().size(),
                definition.isCollectionEnabled(), definition.getCalculationCron(), definition.getConfigVersion());
        return PueDefinitionResponse.from(definition);
    }

    @Transactional
    public PueDefinitionResponse setCollectionEnabled(Integer id, boolean enabled) {
        PueDefinition definition = find(id);
        definition.setCollectionEnabled(enabled);
        definition = repository.save(definition);
        collectorSyncService.sync(definition);
        log.info("[PUE_DEFINITION] action=COLLECTION_TOGGLE definitionId={} enabled={}", id, enabled);
        return PueDefinitionResponse.from(definition);
    }

    @Transactional
    public void delete(Integer id) {
        PueDefinition definition = find(id);
        if (pageWidgetRepository.existsByPueDefinitionId(id)) {
            throw new ConflictException("PUE definition is used by a widget. Delete the widget or select another PUE definition first.");
        }
        collectorSyncService.remove(id);
        repository.delete(definition);
        log.info("[PUE_DEFINITION] action=DELETE definitionId={} name={}", id, definition.getName());
    }

    private PueDefinition find(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("PueDefinition not found: " + id));
    }

    private List<PueDefinition.SourceDefinition> sources(PueDefinitionRequest request) {
        List<PueDefinition.SourceDefinition> all = new ArrayList<>();
        add(all, request.totalSources(), PueDefinitionSourceRole.total);
        add(all, request.coolerSources(), PueDefinitionSourceRole.cooler);
        return all;
    }

    private void add(List<PueDefinition.SourceDefinition> all,
                     List<PueDefinitionSourceRequest> requests,
                     PueDefinitionSourceRole role) {
        for (PueDefinitionSourceRequest request : requests) {
            Device device = deviceRepository.findById(request.deviceId())
                    .orElseThrow(() -> new EntityNotFoundException("Device not found: " + request.deviceId()));
            if (!device.isEnabled()) {
                throw new IllegalArgumentException("PUE source device is disabled: " + request.deviceId());
            }
            all.add(new PueDefinition.SourceDefinition(device, role, request.pointName()));
        }
    }
}
