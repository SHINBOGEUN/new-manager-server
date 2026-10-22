package net.vivans.dcim.module.alarm.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.vivans.dcim.module.collectortask.application.CollectionGroupPlan;
import net.vivans.dcim.module.collectortask.application.CollectionGroupSpecService;
import net.vivans.dcim.module.alarm.domain.AlarmEvent;
import net.vivans.dcim.module.alarm.domain.CollectionGapAlarmDetail;
import net.vivans.dcim.module.alarm.infrastructure.AlarmEventRepository;
import net.vivans.dcim.module.alarm.infrastructure.CollectionGapAlarmDetailRepository;
import net.vivans.dcim.module.collectortask.domain.model.CollectionTask;
import net.vivans.dcim.module.collectortask.domain.model.CollectionTaskDevice;
import net.vivans.dcim.module.collectortask.domain.model.CollectionTaskGroup;
import net.vivans.dcim.module.collectortask.domain.repository.CollectionTaskRepository;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.model.DeviceMonitoringPolicy;
import net.vivans.dcim.module.device.domain.repository.DeviceMonitoringPolicyRepository;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import net.vivans.dcim.module.mqtt.domain.model.DeviceMqttIdentity;
import net.vivans.dcim.module.mqtt.domain.repository.DeviceMqttIdentityRepository;
import net.vivans.dcim.module.query.domain.LastPoint;
import net.vivans.dcim.module.query.domain.PointQuery;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollectionGapEvaluator {
    private final CollectionTaskRepository collectionTasks;
    private final CollectionGroupSpecService collectionGroupSpecService;
    private final DeviceRepository devices;
    private final DeviceMonitoringPolicyRepository monitoringPolicies;
    private final DeviceMqttIdentityRepository mqttIdentities;
    private final PointQuery pointQuery;
    private final AlarmEventRepository events;
    private final CollectionGapAlarmDetailRepository gapDetails;
    private final ApplicationEventPublisher publisher;

    @Transactional
    public void evaluate(Instant now, Duration staleAfter) {
        Map<Integer, DeviceMonitoringPolicy> policiesByDevice = new HashMap<>();
        for (DeviceMonitoringPolicy policy : monitoringPolicies.findAll()) {
            policiesByDevice.put(policy.getDeviceId(), policy);
        }
        Map<TargetKey, Target> targets = resolveTargets(policiesByDevice);
        Instant cutoff = now.minus(staleAfter);
        Map<TargetKey, Instant> latestByTarget = findLatestSamples(targets, cutoff, now);
        Map<TargetKey, CollectionGapAlarmDetail> openByTarget = new HashMap<>();

        for (CollectionGapAlarmDetail detail : gapDetails.findAllOpen()) {
            TargetKey key = new TargetKey(detail.getDevice().getId(), normalizeProtocol(detail.getProtocol()));
            if (!targets.containsKey(key) || latestByTarget.containsKey(key)) {
                detail.getEvent().clear(now);
                continue;
            }
            openByTarget.putIfAbsent(key, detail);
        }

        for (Map.Entry<TargetKey, Target> entry : targets.entrySet()) {
            TargetKey key = entry.getKey();
            Target target = entry.getValue();
            if (latestByTarget.containsKey(key) || target.monitoringSince() == null
                    || target.monitoringSince().isAfter(cutoff) || openByTarget.containsKey(key)) {
                continue;
            }

            AlarmEvent event = events.save(new AlarmEvent("COLLECTION_GAP", "WARNING", "USER_VISIBLE", now));
            gapDetails.save(new CollectionGapAlarmDetail(event, target.device(), key.protocol()));
            publisher.publishEvent(new AlarmCreated(event.getId()));
            log.info("collection gap alarm created deviceId={} protocol={} staleAfterMinutes={}",
                    key.deviceId(), key.protocol(), staleAfter.toMinutes());
        }
    }

    private Map<TargetKey, Instant> findLatestSamples(Map<TargetKey, Target> targets, Instant cutoff, Instant now) {
        Map<String, List<Integer>> deviceIdsByProtocol = new HashMap<>();
        targets.keySet().forEach(key -> deviceIdsByProtocol
                .computeIfAbsent(key.protocol(), ignored -> new ArrayList<>()).add(key.deviceId()));
        Map<TargetKey, Instant> latest = new HashMap<>();
        for (Map.Entry<String, List<Integer>> entry : deviceIdsByProtocol.entrySet()) {
            List<Integer> deviceIds = entry.getValue().stream().distinct().toList();
            for (LastPoint point : pointQuery.findLastInRange(deviceIds, List.of(), cutoff, now, entry.getKey())) {
                TargetKey key = new TargetKey(point.deviceId(), entry.getKey());
                Target target = targets.get(key);
                if (point.time() != null && target != null && !point.time().isBefore(cutoff)) latest.merge(key, point.time(),
                        (first, second) -> first.isAfter(second) ? first : second);
            }
        }
        return latest;
    }

    private Map<TargetKey, Target> resolveTargets(Map<Integer, DeviceMonitoringPolicy> policies) {
        Map<TargetKey, Target> targets = new HashMap<>();
        for (CollectionTask task : collectionTasks.findAll(null, null, true)) {
            String protocol = task.getScriptType().getCode();
            if (!"snmp".equalsIgnoreCase(protocol) && !"modbus".equalsIgnoreCase(protocol)) continue;
            for (CollectionTaskGroup group : task.getGroups()) {
                if (!group.isActive() || isBlank(group.getCollectorJobId())) continue;
                CollectionGroupPlan plan = collectionGroupSpecService.generate(group);
                Map<Integer, Instant> sourceMonitoringSince = new HashMap<>();
                for (CollectionTaskDevice taskDevice : group.getDevices()) {
                    sourceMonitoringSince.put(taskDevice.getDevice().getId(), taskDevice.getCreatedDt());
                }
                plan.pointsBySourceDeviceId().forEach((sourceDeviceId, points) -> {
                    Instant monitoringSince = sourceMonitoringSince.get(sourceDeviceId);
                    points.stream().map(CollectionGroupPlan.PointSource::storageDeviceId)
                            .filter(java.util.Objects::nonNull).distinct()
                            .forEach(storageDeviceId -> devices.findById(storageDeviceId)
                                    .filter(Device::isEnabled)
                                    .ifPresent(device -> addTarget(targets, device, protocol, monitoringSince,
                                            policies.get(device.getId()))));
                });
            }
        }

        for (DeviceMqttIdentity identity : mqttIdentities.findAllOrderByIdAsc()) {
            Device device = identity.getDevice();
            if (identity.isEnabled() && device != null && device.isEnabled()) {
                addTarget(targets, device, "mqtt", identity.getCreatedDt(), policies.get(device.getId()));
            }
        }
        return targets;
    }

    private static void addTarget(Map<TargetKey, Target> targets, Device device, String protocol, Instant since,
                                  DeviceMonitoringPolicy policy) {
        if (policy != null && "ON_CHANGE".equals(policy.getMonitoringMode().getCode())) return;
        TargetKey key = new TargetKey(device.getId(), normalizeProtocol(protocol));
        targets.merge(key, new Target(device, since), (first, second) -> {
            Instant earliest = first.monitoringSince() == null ? second.monitoringSince()
                    : second.monitoringSince() == null || first.monitoringSince().isBefore(second.monitoringSince())
                    ? first.monitoringSince() : second.monitoringSince();
            return new Target(first.device(), earliest);
        });
    }

    private static String normalizeProtocol(String protocol) {
        return protocol == null ? "" : protocol.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record TargetKey(Integer deviceId, String protocol) {}
    private record Target(Device device, Instant monitoringSince) {}
}
