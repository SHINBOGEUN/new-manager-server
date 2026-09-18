package net.vivans.dcim.module.collectortask.application;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.vivans.dcim.module.collectortask.api.dto.CollectionTaskCreateRequest;
import net.vivans.dcim.module.collectortask.api.dto.CollectionTaskGroupRequest;
import net.vivans.dcim.module.collectortask.api.dto.CollectionTaskGroupResponse;
import net.vivans.dcim.module.collectortask.api.dto.CollectionTaskResponse;
import net.vivans.dcim.module.collectortask.api.dto.CollectionTaskUpdateRequest;
import net.vivans.dcim.module.collectortask.domain.model.CollectionTask;
import net.vivans.dcim.module.collectortask.domain.model.CollectionTaskGroup;
import net.vivans.dcim.module.collectortask.domain.repository.CollectionTaskRepository;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.common.domain.repository.CommonCodeRepository;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModel;
import net.vivans.dcim.module.devicemodel.domain.repository.DeviceModelRepository;
import net.vivans.dcim.shared.exception.ConflictException;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CollectionTaskService {

    private static final String SCRIPT_TYPE_GROUP_KEY = "PROTOCOL_TYPE";
    private static final String TASK_ALREADY_EXISTS_MESSAGE = "collection task already exists for this model and script type";
    private static final String DEVICE_ALREADY_IN_GROUP_MESSAGE = "device already belongs to another group in this task";
    private static final String DEVICE_MODEL_MISMATCH_MESSAGE = "device does not belong to task model";
    private static final String DUPLICATE_CRON_MESSAGE = "cronExpression already exists in this task";
    private static final String DUPLICATE_DEVICE_IN_REQUEST_MESSAGE = "duplicate deviceId in request";

    private final CollectionTaskRepository collectionTaskRepository;
    private final CollectionGroupSpecService collectionGroupSpecService;
    private final CollectionScriptSyncService collectionScriptSyncService;
    private final CollectorSyncService collectorSyncService;
    private final CommonCodeRepository commonCodeRepository;
    private final DeviceModelRepository deviceModelRepository;
    private final DeviceRepository deviceRepository;
    private final EntityManager entityManager;

    public List<CollectionTaskResponse> getTasks(Integer modelId, Integer scriptTypeId, Boolean active) {
        List<CollectionTaskResponse> responses = new ArrayList<>();
        for (CollectionTask task : collectionTaskRepository.findAll(modelId, scriptTypeId, active)) {
            responses.add(CollectionTaskResponse.from(task, collectionGroupSpecService));
        }
        return responses;
    }

    public CollectionTaskResponse getTask(Integer taskId) {
        return CollectionTaskResponse.from(findTask(taskId), collectionGroupSpecService);
    }

    @Transactional
    public CollectionTaskResponse createTask(CollectionTaskCreateRequest request) {
        DeviceModel deviceModel = findDeviceModel(request.modelId());
        CommonCode scriptType = findScriptType(request.scriptTypeId());
        if (collectionTaskRepository.existsByModelIdAndScriptTypeId(deviceModel.getId(), scriptType.getId())) {
            throw new ConflictException(TASK_ALREADY_EXISTS_MESSAGE);
        }

        boolean active = request.active() == null || request.active();
        CollectionTask task = CollectionTask.create(request.name(), deviceModel, scriptType, active);
        addGroups(task, request.groups());
        collectionTaskRepository.saveAndFlush(task);
        if (shouldAutoAssignDevices(request.groups())) {
            collectionScriptSyncService.assignUnassignedModelDevicesAndRegenerate(task);
        } else {
            collectionScriptSyncService.regenerateTask(task);
        }
        return CollectionTaskResponse.from(task, collectionGroupSpecService);
    }

    @Transactional
    public CollectionTaskResponse updateTask(Integer taskId, CollectionTaskUpdateRequest request) {
        CollectionTask task = findTask(taskId);
        task.update(request.name(), request.active());
        collectionTaskRepository.save(task);
        collectorSyncService.syncTaskToggle(task);
        return CollectionTaskResponse.from(task, collectionGroupSpecService);
    }

    @Transactional
    public Integer deleteTask(Integer taskId) {
        CollectionTask task = findTask(taskId);
        collectorSyncService.removeTaskJobs(task);
        collectionTaskRepository.delete(task);
        return taskId;
    }

    @Transactional
    public CollectionTaskResponse toggleTask(Integer taskId) {
        CollectionTask task = findTask(taskId);
        task.toggleActive();
        collectionTaskRepository.save(task);
        collectorSyncService.syncTaskToggle(task);
        return CollectionTaskResponse.from(task, collectionGroupSpecService);
    }

    @Transactional
    public CollectionTaskGroupResponse createGroup(Integer taskId, CollectionTaskGroupRequest request) {
        CollectionTask task = findTask(taskId);
        CollectionTaskGroup group = buildGroup(task, request, null);
        // The task is already managed, but the newly-created child must be
        // explicitly made persistent before its generated id is used in the
        // collector spec.
        entityManager.persist(group);
        collectionTaskRepository.saveAndFlush(task);
        // The collector spec contains the generated collection_task_group.id.
        // Force the cascade insert before generating the spec so a newly-created
        // group can never be synchronized with a null groupId.
        entityManager.flush();
        if (group.getId() == null) {
            throw new IllegalStateException("collection task group id was not generated");
        }
        group.updateGeneratedSpec(collectionGroupSpecService.generateJson(group));
        collectionTaskRepository.saveAndFlush(task);
        collectorSyncService.syncGroupSpec(group);
        return CollectionTaskGroupResponse.from(group, collectionGroupSpecService);
    }

    @Transactional
    public CollectionTaskGroupResponse updateGroup(
            Integer taskId,
            Integer groupId,
            CollectionTaskGroupRequest request
    ) {
        CollectionTask task = findTask(taskId);
        CollectionTaskGroup group = findGroup(task, groupId);
        applyGroupUpdate(task, group, request);
        collectionTaskRepository.saveAndFlush(task);
        group.updateGeneratedSpec(collectionGroupSpecService.generateJson(group));
        collectionTaskRepository.saveAndFlush(task);
        collectorSyncService.syncGroupSpec(group);
        return CollectionTaskGroupResponse.from(group, collectionGroupSpecService);
    }

    @Transactional
    public Integer deleteGroup(Integer taskId, Integer groupId) {
        CollectionTask task = findTask(taskId);
        CollectionTaskGroup group = findGroup(task, groupId);
        collectorSyncService.removeGroupJob(group);
        task.getGroups().remove(group);
        collectionTaskRepository.save(task);
        return groupId;
    }

    /**
     * 그룹-장비 연결 "한 건"만 제거한다. {@link #applyGroupUpdate}처럼 전체 deviceIds를
     * 다시 검증(resolveDevices)하지 않으므로, 같은 그룹의 다른 장비 연결에는 전혀 영향을 주지
     * 않는다. 모델이 다른(수집 대상에서 제외된) 연결을 정리할 때 이 메서드를 쓴다 — 정상
     * 장비의 전체 그룹 편집(updateGroup)과는 별개의, 단건 삭제 전용 경로다.
     */
    @Transactional
    public CollectionTaskGroupResponse removeGroupDevice(Integer taskId, Integer groupId, Integer deviceId) {
        CollectionTask task = findTask(taskId);
        CollectionTaskGroup group = findGroup(task, groupId);
        if (!group.containsDevice(deviceId)) {
            throw new EntityNotFoundException(
                    "device " + deviceId + " is not connected to group " + groupId);
        }
        group.removeDevice(deviceId);
        collectionTaskRepository.saveAndFlush(task);
        group.updateGeneratedSpec(collectionGroupSpecService.generateJson(group));
        collectionTaskRepository.saveAndFlush(task);
        collectorSyncService.syncGroupSpec(group);
        return CollectionTaskGroupResponse.from(group, collectionGroupSpecService);
    }

    @Transactional
    public CollectionTaskGroupResponse toggleGroup(Integer taskId, Integer groupId) {
        CollectionTask task = findTask(taskId);
        CollectionTaskGroup group = findGroup(task, groupId);
        group.toggleActive();
        collectionTaskRepository.save(task);
        collectorSyncService.syncGroupToggle(group);
        return CollectionTaskGroupResponse.from(group, collectionGroupSpecService);
    }

    private void addGroups(CollectionTask task, List<CollectionTaskGroupRequest> groupRequests) {
        if (groupRequests == null || groupRequests.isEmpty()) {
            return;
        }
        for (CollectionTaskGroupRequest groupRequest : groupRequests) {
            buildGroup(task, groupRequest, null);
        }
    }

    private CollectionTaskGroup buildGroup(
            CollectionTask task,
            CollectionTaskGroupRequest request,
            Integer excludeGroupId
    ) {
        validateCronExpression(request.cronExpression());
        if (task.hasCronExpression(request.cronExpression(), excludeGroupId)) {
            throw new ConflictException(DUPLICATE_CRON_MESSAGE);
        }
        boolean active = request.active() == null || request.active();
        CollectionTaskGroup group = CollectionTaskGroup.create(
                task,
                request.name(),
                request.cronExpression(),
                active
        );
        group.replaceDevices(resolveDevices(task, request.deviceIds(), excludeGroupId));
        return group;
    }

    private void applyGroupUpdate(
            CollectionTask task,
            CollectionTaskGroup group,
            CollectionTaskGroupRequest request
    ) {
        validateCronExpression(request.cronExpression());
        if (task.hasCronExpression(request.cronExpression(), group.getId())) {
            throw new ConflictException(DUPLICATE_CRON_MESSAGE);
        }
        boolean active = request.active() == null || request.active();
        group.update(request.name(), request.cronExpression(), active);
        if (request.deviceIds() != null) {
            group.replaceDevices(resolveDevices(task, request.deviceIds(), group.getId()));
        }
    }

    private List<Device> resolveDevices(CollectionTask task, List<Integer> deviceIds, Integer excludeGroupId) {
        if (deviceIds == null || deviceIds.isEmpty()) {
            return List.of();
        }
        Set<Integer> uniqueIds = new HashSet<>();
        List<Device> devices = new ArrayList<>();
        Integer modelId = task.getDeviceModel().getId();
        for (Integer deviceId : deviceIds) {
            if (deviceId == null) {
                continue;
            }
            if (!uniqueIds.add(deviceId)) {
                throw new IllegalArgumentException(DUPLICATE_DEVICE_IN_REQUEST_MESSAGE);
            }
            Device device = deviceRepository.findById(deviceId)
                    .orElseThrow(() -> new EntityNotFoundException("Device not found: " + deviceId));
            if (!modelId.equals(device.getDeviceModel().getId())) {
                throw new IllegalArgumentException(DEVICE_MODEL_MISMATCH_MESSAGE);
            }
            if (task.containsDevice(deviceId, excludeGroupId)) {
                throw new ConflictException(DEVICE_ALREADY_IN_GROUP_MESSAGE);
            }
            devices.add(device);
        }
        return devices;
    }

    private CollectionTask findTask(Integer taskId) {
        return collectionTaskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("CollectionTask not found: " + taskId));
    }

    private CollectionTaskGroup findGroup(CollectionTask task, Integer groupId) {
        for (CollectionTaskGroup group : task.getGroups()) {
            if (groupId.equals(group.getId())) {
                return group;
            }
        }
        throw new EntityNotFoundException("CollectionTaskGroup not found: " + groupId);
    }

    private DeviceModel findDeviceModel(Integer modelId) {
        return deviceModelRepository.findById(modelId)
                .orElseThrow(() -> new EntityNotFoundException("DeviceModel not found: " + modelId));
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

    private boolean shouldAutoAssignDevices(List<CollectionTaskGroupRequest> groupRequests) {
        return groupRequests == null || groupRequests.isEmpty();
    }
}
