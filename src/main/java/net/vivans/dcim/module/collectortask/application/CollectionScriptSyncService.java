package net.vivans.dcim.module.collectortask.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.vivans.dcim.module.collectortask.domain.model.CollectionTask;
import net.vivans.dcim.module.collectortask.domain.model.CollectionTaskGroup;
import net.vivans.dcim.module.collectortask.domain.repository.CollectionTaskRepository;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import net.vivans.dcim.module.device.domain.repository.DeviceModbusReadingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollectionScriptSyncService {

    private final CollectionTaskRepository collectionTaskRepository;
    private final CollectionGroupSpecService collectionGroupSpecService;
    private final DeviceRepository deviceRepository;
    private final DeviceModbusReadingRepository deviceModbusReadingRepository;
    private final CollectorSyncService collectorSyncService;
    @org.springframework.beans.factory.annotation.Autowired
    private CollectionIntervalResolver intervalResolver;

    @Transactional
    public void regenerateByModelId(Integer modelId) {
        if (modelId == null) {
            return;
        }
        List<CollectionTask> tasks = collectionTaskRepository.findAllByModelId(modelId);
        for (CollectionTask task : tasks) {
            regenerateTask(task);
        }
    }

    /** 수집원과 다른 장비에 값을 저장하는 회선 매핑의 대상이 변경됐을 때 수집원 작업을 갱신한다. */
    @Transactional
    public void regenerateForMappedTarget(Integer targetDeviceId) {
        if (targetDeviceId == null) {
            return;
        }
        Set<Integer> sourceModelIds = deviceModbusReadingRepository
                .findAllByTargetDeviceIdOrderByIdAsc(targetDeviceId).stream()
                .map(reading -> reading.getEndpointModbus().getEndpoint().getDevice().getDeviceModel().getId())
                .collect(Collectors.toSet());
        sourceModelIds.forEach(this::regenerateByModelId);
    }

    @Transactional
    public void regenerateTask(CollectionTask task) {
        collectionTaskRepository.saveAndFlush(task);
        for (CollectionTaskGroup group : new ArrayList<>(task.getGroups())) {
            String spec = collectionGroupSpecService.generateJson(group);
            if (Objects.equals(spec, group.getGeneratedSpec())) {
                continue;
            }
            group.updateGeneratedSpec(spec);
            log.info("regenerated collection group spec: taskId={}, groupId={}", task.getId(), group.getId());
            collectorSyncService.syncGroupSpec(group, false);
        }
        collectionTaskRepository.save(task);
    }

    @Transactional
    public void assignDeviceAndRegenerate(Device device) {
        if (device == null || device.getId() == null || device.getDeviceModel() == null) {
            return;
        }
        List<CollectionTask> tasks = collectionTaskRepository.findAllByModelId(device.getDeviceModel().getId());
        for (CollectionTask task : tasks) {
            assignToDefaultGroup(task, device);
            regenerateTask(task);
        }
    }

    /** 복합 등록에서는 선택된 프로토콜의 작업에만 기본 그룹 연결을 만든다. */
    @Transactional
    public void assignDeviceAndRegenerate(Device device, String protocolCode) {
        if (device == null || device.getId() == null || device.getDeviceModel() == null) {
            return;
        }
        for (CollectionTask task : collectionTaskRepository.findAllByModelId(device.getDeviceModel().getId())) {
            if (!task.getScriptType().getCode().equalsIgnoreCase(protocolCode)) {
                continue;
            }
            assignToDefaultGroup(task, device);
            regenerateTask(task);
        }
    }

    @Transactional
    public void assignUnassignedModelDevicesAndRegenerate(CollectionTask task) {
        if (task == null || task.getDeviceModel() == null) {
            return;
        }
        ensureDefaultGroup(task);
        List<Device> devices = deviceRepository.findAllByDeviceModelId(task.getDeviceModel().getId());
        for (Device device : devices) {
            assignToDefaultGroup(task, device);
        }
        regenerateTask(task);
    }

    @Transactional
    public void removeDeviceAndRegenerate(Integer deviceId, Integer modelId) {
        if (deviceId == null || modelId == null) {
            return;
        }
        List<CollectionTask> tasks = collectionTaskRepository.findAllByModelId(modelId);
        for (CollectionTask task : tasks) {
            for (CollectionTaskGroup group : task.getGroups()) {
                group.removeDevice(deviceId);
            }
            regenerateTask(task);
        }
    }

    private CollectionTaskGroup ensureDefaultGroup(CollectionTask task) {
        CollectionTaskGroup existing=task.defaultGroup();
        if(existing!=null)return existing;
        var interval=intervalResolver.defaultInterval();
        return task.ensureDefaultGroup(CollectionTask.DEFAULT_GROUP_NAME,interval.cron());
    }

    private void assignToDefaultGroup(CollectionTask task, Device device) {
        if (device == null || device.getId() == null) {
            return;
        }
        if (task.containsDevice(device.getId(), null)) {
            return;
        }
        ensureDefaultGroup(task).addDevice(device);
    }
}
