package net.vivans.dcim.module.collectortask.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.vivans.dcim.module.collectortask.domain.model.CollectionTask;
import net.vivans.dcim.module.collectortask.domain.model.CollectionTaskGroup;
import net.vivans.dcim.module.collectortask.domain.repository.CollectionTaskRepository;
import net.vivans.dcim.module.collectortask.infrastructure.collector.CollectorJobClient;
import net.vivans.dcim.module.collectortask.infrastructure.collector.CollectorJobResponse;
import net.vivans.dcim.shared.exception.CollectorSyncException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollectorSyncService {

    private final CollectorJobClient collectorJobClient;
    private final CollectionTaskRepository collectionTaskRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void syncGroupSpec(CollectionTaskGroup group) {
        syncGroupSpec(group, collectorJobClient.isFailFast());
    }

    @Transactional
    public void syncGroupSpec(CollectionTaskGroup group, boolean failFast) {
        if (!collectorJobClient.isEnabled()) {
            return;
        }
        CollectionTask task = group.getTask();
        String specJson = group.getGeneratedSpec();
        if (specJson == null || specJson.isBlank()) {
            return;
        }
        if (!isSnmpSpec(specJson)) {
            removeGroupJob(group, failFast);
            save(task);
            return;
        }
        if (!isCollectorEnabled(task, group)) {
            disableGroupJob(group, failFast);
            save(task);
            return;
        }
        try {
            if (group.getCollectorJobId() == null) {
                CollectorJobResponse response = collectorJobClient.register(specJson);
                group.updateCollectorJobId(response.collectorJobId());
                log.info(
                        "collector job registered: taskId={}, groupId={}, collectorJobId={}",
                        task.getId(),
                        group.getId(),
                        response.collectorJobId()
                );
            } else {
                CollectorJobResponse response = collectorJobClient.update(group.getCollectorJobId(), specJson);
                group.updateCollectorJobId(response.collectorJobId());
                log.info(
                        "collector job updated: taskId={}, groupId={}, collectorJobId={}",
                        task.getId(),
                        group.getId(),
                        response.collectorJobId()
                );
            }
            save(task);
        } catch (Exception exception) {
            handleFailure("syncGroupSpec", task.getId(), group.getId(), failFast, exception);
        }
    }

    @Transactional
    public void syncGroupToggle(CollectionTaskGroup group) {
        syncGroupToggle(group, collectorJobClient.isFailFast());
    }

    @Transactional
    public void syncGroupToggle(CollectionTaskGroup group, boolean failFast) {
        if (!collectorJobClient.isEnabled()) {
            return;
        }
        CollectionTask task = group.getTask();
        if (!isSnmpTask(task)) {
            removeGroupJob(group, failFast);
            save(task);
            return;
        }
        boolean enabled = isCollectorEnabled(task, group);
        if (group.getCollectorJobId() == null) {
            if (enabled && hasGeneratedSpec(group)) {
                syncGroupSpec(group, failFast);
            }
            return;
        }
        try {
            collectorJobClient.toggle(group.getCollectorJobId(), enabled);
            log.info(
                    "collector job toggled: taskId={}, groupId={}, collectorJobId={}, enabled={}",
                    task.getId(),
                    group.getId(),
                    group.getCollectorJobId(),
                    enabled
            );
        } catch (Exception exception) {
            handleFailure("syncGroupToggle", task.getId(), group.getId(), failFast, exception);
        }
    }

    @Transactional
    public void syncTaskToggle(CollectionTask task) {
        syncTaskToggle(task, collectorJobClient.isFailFast());
    }

    @Transactional
    public void syncTaskToggle(CollectionTask task, boolean failFast) {
        for (CollectionTaskGroup group : new ArrayList<>(task.getGroups())) {
            syncGroupToggle(group, failFast);
        }
    }

    @Transactional
    public void removeGroupJob(CollectionTaskGroup group) {
        removeGroupJob(group, collectorJobClient.isFailFast());
    }

    @Transactional
    public void removeGroupJob(CollectionTaskGroup group, boolean failFast) {
        if (!collectorJobClient.isEnabled()) {
            return;
        }
        if (group.getCollectorJobId() == null) {
            return;
        }
        CollectionTask task = group.getTask();
        String collectorJobId = group.getCollectorJobId();
        try {
            collectorJobClient.delete(collectorJobId);
            group.updateCollectorJobId(null);
            save(task);
            log.info(
                    "collector job deleted: taskId={}, groupId={}, collectorJobId={}",
                    task.getId(),
                    group.getId(),
                    collectorJobId
            );
        } catch (Exception exception) {
            handleFailure("removeGroupJob", task.getId(), group.getId(), failFast, exception);
        }
    }

    @Transactional
    public void removeTaskJobs(CollectionTask task) {
        removeTaskJobs(task, collectorJobClient.isFailFast());
    }

    @Transactional
    public void removeTaskJobs(CollectionTask task, boolean failFast) {
        for (CollectionTaskGroup group : new ArrayList<>(task.getGroups())) {
            removeGroupJob(group, failFast);
        }
    }

    @Transactional
    public void repushActiveGroups() {
        if (!collectorJobClient.isEnabled()) {
            log.info("collector sync disabled; skip startup repush");
            return;
        }
        List<CollectionTask> tasks = collectionTaskRepository.findAll(null, null, null);
        for (CollectionTask task : tasks) {
            if (!task.isActive()) {
                continue;
            }
            if (!isSnmpTask(task)) {
                continue;
            }
            for (CollectionTaskGroup group : new ArrayList<>(task.getGroups())) {
                if (!group.isActive() || !hasGeneratedSpec(group)) {
                    continue;
                }
                repushGroupInternal(group);
            }
        }
    }

    @Transactional
    public void repushGroup(CollectionTaskGroup group) {
        repushGroupInternal(group);
    }

    private void repushGroupInternal(CollectionTaskGroup group) {
        CollectionTask task = group.getTask();
        String specJson = group.getGeneratedSpec();
        if (specJson == null || specJson.isBlank()) {
            return;
        }
        try {
            CollectorJobResponse response = collectorJobClient.register(specJson);
            group.updateCollectorJobId(response.collectorJobId());
            save(task);
            log.info(
                    "collector job repushed: taskId={}, groupId={}, collectorJobId={}",
                    task.getId(),
                    group.getId(),
                    response.collectorJobId()
            );
        } catch (Exception exception) {
            handleFailure("repushGroup", task.getId(), group.getId(), false, exception);
        }
    }

    private void disableGroupJob(CollectionTaskGroup group, boolean failFast) {
        if (group.getCollectorJobId() == null) {
            return;
        }
        CollectionTask task = group.getTask();
        try {
            collectorJobClient.toggle(group.getCollectorJobId(), false);
            log.info(
                    "collector job disabled: taskId={}, groupId={}, collectorJobId={}",
                    task.getId(),
                    group.getId(),
                    group.getCollectorJobId()
            );
        } catch (Exception exception) {
            handleFailure("disableGroupJob", task.getId(), group.getId(), failFast, exception);
        }
    }

    private void handleFailure(
            String operation,
            Integer taskId,
            Integer groupId,
            boolean failFast,
            Exception exception
    ) {
        collectorJobClient.logFailure(operation, taskId, groupId, exception);
        if (failFast) {
            throw new CollectorSyncException(
                    "collector sync failed: " + operation + " (taskId=" + taskId + ", groupId=" + groupId + ")",
                    exception
            );
        }
    }

    private void save(CollectionTask task) {
        collectionTaskRepository.save(task);
    }

    private static boolean isCollectorEnabled(CollectionTask task, CollectionTaskGroup group) {
        return task.isActive() && group.isActive();
    }

    private static boolean hasGeneratedSpec(CollectionTaskGroup group) {
        return group.getGeneratedSpec() != null && !group.getGeneratedSpec().isBlank();
    }

    private static boolean isSnmpTask(CollectionTask task) {
        return CollectionGroupSpecService.SNMP_PROTOCOL_CODE.equalsIgnoreCase(task.getScriptType().getCode());
    }

    private boolean isSnmpSpec(String specJson) {
        try {
            CollectionGroupSpec spec = objectMapper.readValue(specJson, CollectionGroupSpec.class);
            return CollectionGroupSpecService.SNMP_PROTOCOL_CODE.equalsIgnoreCase(spec.protocol());
        } catch (Exception exception) {
            log.warn("failed to parse generated spec for collector sync", exception);
            return false;
        }
    }
}
