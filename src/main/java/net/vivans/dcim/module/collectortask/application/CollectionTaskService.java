package net.vivans.dcim.module.collectortask.application;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.collectortask.api.dto.CollectionTaskCreateRequest;
import net.vivans.dcim.module.collectortask.api.dto.CollectionTaskResponse;
import net.vivans.dcim.module.collectortask.api.dto.CollectionTaskUpdateRequest;
import net.vivans.dcim.module.collectortask.domain.model.CollectionTask;
import net.vivans.dcim.module.collectortask.domain.repository.CollectionTaskRepository;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.common.domain.repository.CommonCodeRepository;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CollectionTaskService {

    private static final String SCRIPT_TYPE_GROUP_KEY = "PROTOCOL_TYPE";

    private final CollectionTaskRepository collectionTaskRepository;
    private final CommonCodeRepository commonCodeRepository;

    public List<CollectionTaskResponse> getTasks(String name, Boolean active, Integer scriptTypeId) {
        List<CollectionTask> tasks = collectionTaskRepository.findAll(name, active, scriptTypeId);
        List<CollectionTaskResponse> responses = new ArrayList<>();
        for (CollectionTask task : tasks) {
            responses.add(CollectionTaskResponse.from(task));
        }
        return responses;
    }

    public CollectionTaskResponse getTask(String taskId) {
        return CollectionTaskResponse.from(findTask(taskId));
    }

    @Transactional
    public CollectionTaskResponse createTask(CollectionTaskCreateRequest request) {
        validateCronExpression(request.cronExpression());
        CommonCode scriptType = findScriptType(request.scriptTypeId());
        boolean active = request.active() == null || request.active();
        CollectionTask task = CollectionTask.create(
                request.name(),
                request.cronExpression(),
                scriptType,
                active
        );
        return CollectionTaskResponse.from(collectionTaskRepository.save(task));
    }

    @Transactional
    public CollectionTaskResponse updateTask(String taskId, CollectionTaskUpdateRequest request) {
        CollectionTask task = findTask(taskId);
        validateCronExpression(request.cronExpression());
        CommonCode scriptType = findScriptType(request.scriptTypeId());
        task.update(request.name(), request.cronExpression(), scriptType, request.active());
        return CollectionTaskResponse.from(collectionTaskRepository.save(task));
    }

    @Transactional
    public String deleteTask(String taskId) {
        CollectionTask task = findTask(taskId);
        collectionTaskRepository.delete(task);
        return taskId;
    }

    @Transactional
    public CollectionTaskResponse toggleTask(String taskId) {
        CollectionTask task = findTask(taskId);
        task.toggleActive();
        return CollectionTaskResponse.from(collectionTaskRepository.save(task));
    }

    private CollectionTask findTask(String taskId) {
        return collectionTaskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("CollectionTask not found: " + taskId));
    }

    private CommonCode findScriptType(Integer scriptTypeId) {
        CommonCode scriptType = commonCodeRepository.findById(scriptTypeId)
                .orElseThrow(() -> new EntityNotFoundException("CommonCode not found: " + scriptTypeId));
        if (!SCRIPT_TYPE_GROUP_KEY.equals(scriptType.getCodeGroup().getGroupKey())) {
            throw new IllegalArgumentException("scriptType must belong to PROTOCOL_TYPE group");
        }
        return scriptType;
    }

    private void validateCronExpression(String cronExpression) {
        if (!CronExpression.isValidExpression(cronExpression)) {
            throw new IllegalArgumentException("cronExpression is invalid");
        }
    }
}
