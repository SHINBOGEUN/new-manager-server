package net.vivans.dcim.module.collectortask.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.vivans.dcim.module.collectortask.domain.model.CollectionTask;
import net.vivans.dcim.module.collectortask.domain.model.CollectionTaskGroup;
import net.vivans.dcim.module.collectortask.domain.repository.CollectionTaskRepository;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollectionScriptSyncService {

    private final CollectionTaskRepository collectionTaskRepository;
    private final CollectionGroupSpecService collectionGroupSpecService;
    private final DeviceRepository deviceRepository;

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

    @Transactional
    public void regenerateTask(CollectionTask task) {
        collectionTaskRepository.save(task);
        for (CollectionTaskGroup group : task.getGroups()) {
            String spec = collectionGroupSpecService.generateJson(group);
            if (Objects.equals(spec, group.getGeneratedSpec())) {
                continue;
            }
            group.updateGeneratedSpec(spec);
            log.info("regenerated collection group spec: taskId={}, groupId={}", task.getId(), group.getId());
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

    @Transactional
    public void assignUnassignedModelDevicesAndRegenerate(CollectionTask task) {
        if (task == null || task.getDeviceModel() == null) {
            return;
        }
        task.ensureDefaultGroup();
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

    private void assignToDefaultGroup(CollectionTask task, Device device) {
        if (device == null || device.getId() == null) {
            return;
        }
        if (task.containsDevice(device.getId(), null)) {
            return;
        }
        task.ensureDefaultGroup().addDevice(device);
    }
}
