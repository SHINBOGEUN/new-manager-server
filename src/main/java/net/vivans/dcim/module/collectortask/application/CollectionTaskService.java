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

    public List<CollectionTaskResponse> getTasks(Integer modelId, Integer scriptTypeId, Boolean active) {
        List<CollectionTaskResponse> responses = new ArrayList<>();
        for (CollectionTask task : collectionTaskRepository.findAll(modelId, scriptTypeId, active)) {
            responses.add(CollectionTaskResponse.from(task));
        }
        return responses;
    }

    public CollectionTaskResponse getTask(Integer taskId) {
        return CollectionTaskResponse.from(findTask(taskId));
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
        collectionScriptSyncService.assignUnassignedModelDevicesAndRegenerate(task);
        return CollectionTaskResponse.from(task);
    }

    @Transactional
    public CollectionTaskResponse updateTask(Integer taskId, CollectionTaskUpdateRequest request) {
        CollectionTask task = findTask(taskId);
        task.update(request.name(), request.active());
        collectionTaskRepository.save(task);
        collectorSyncService.syncTaskToggle(task);
        return CollectionTaskResponse.from(task);
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
        return CollectionTaskResponse.from(task);
    }

    @Transactional
    public CollectionTaskGroupResponse createGroup(Integer taskId, CollectionTaskGroupRequest request) {
        CollectionTask task = findTask(taskId);
        CollectionTaskGroup group = buildGroup(task, request, null);
        collectionTaskRepository.saveAndFlush(task);
        group.updateGeneratedSpec(collectionGroupSpecService.generateJson(group));
        collectionTaskRepository.saveAndFlush(task);
        collectorSyncService.syncGroupSpec(group);
        return CollectionTaskGroupResponse.from(group);
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
        return CollectionTaskGroupResponse.from(group);
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

    @Transactional
    public CollectionTaskGroupResponse toggleGroup(Integer taskId, Integer groupId) {
        CollectionTask task = findTask(taskId);
        CollectionTaskGroup group = findGroup(task, groupId);
        group.toggleActive();
        collectionTaskRepository.save(task);
        collectorSyncService.syncGroupToggle(group);
        return CollectionTaskGroupResponse.from(group);
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
}
