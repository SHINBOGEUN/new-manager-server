package net.vivans.dcim.module.query.application;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.collectortask.domain.model.CollectionTask;
import net.vivans.dcim.module.collectortask.domain.model.CollectionTaskDevice;
import net.vivans.dcim.module.collectortask.domain.model.CollectionTaskGroup;
import net.vivans.dcim.module.collectortask.domain.repository.CollectionTaskRepository;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModelSnmpPoint;
import net.vivans.dcim.module.devicemodel.domain.repository.DeviceModelSnmpPointRepository;
import net.vivans.dcim.module.query.api.dto.CollectionDeviceStatusResponse;
import net.vivans.dcim.module.query.api.dto.CollectionStatusResponse;
import net.vivans.dcim.module.query.domain.LastPoint;
import net.vivans.dcim.module.query.domain.PointQuery;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 장비 등록·수집 작업·Influx 최신 데이터를 한 화면의 운영 상태로 결합한다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CollectionStatusQueryService {
    private static final int MAX_DEVICES = 200;
    private static final int DEFAULT_LOOKBACK_HOURS = 168;
    private static final int MAX_LOOKBACK_HOURS = 168;

    private final DeviceRepository deviceRepository;
    private final CollectionTaskRepository collectionTaskRepository;
    private final DeviceModelSnmpPointRepository modelPointRepository;
    private final PointQuery pointQuery;

    public CollectionStatusResponse getStatus(Integer lookbackHours) {
        Instant now = Instant.now();
        int resolvedLookbackHours = resolveLookbackHours(lookbackHours);
        List<Device> devices = deviceRepository.findAll(null, null, null, null, null, null,
                PageRequest.of(0, MAX_DEVICES, org.springframework.data.domain.Sort.by("id"))).getContent();
        Map<Integer, Registration> registrationByDeviceId = registrations();
        Map<Integer, List<DeviceModelSnmpPoint>> pointsByModelId = pointsByModel(devices);
        Map<Integer, List<LastPoint>> lastPointsByDeviceId = latestPoints(devices, registrationByDeviceId,
                pointsByModelId, Duration.ofHours(resolvedLookbackHours));

        List<CollectionDeviceStatusResponse> rows = new ArrayList<>();
        for (Device device : devices) {
            Registration registration = registrationByDeviceId.get(device.getId());
            List<DeviceModelSnmpPoint> modelPoints = pointsByModelId.getOrDefault(device.getDeviceModel().getId(), List.of());
            List<LastPoint> latestPoints = lastPointsByDeviceId.getOrDefault(device.getId(), List.of());
            rows.add(toResponse(device, registration, modelPoints, latestPoints, now));
        }
        rows.sort(Comparator.comparing(CollectionDeviceStatusResponse::status)
                .thenComparing(CollectionDeviceStatusResponse::deviceName));
        return new CollectionStatusResponse(now, summarize(rows), rows);
    }

    private Map<Integer, Registration> registrations() {
        Map<Integer, Registration> result = new HashMap<>();
        for (CollectionTask task : collectionTaskRepository.findAll(null, null, null)) {
            for (CollectionTaskGroup group : task.getGroups()) {
                for (CollectionTaskDevice mapping : group.getDevices()) {
                    Registration candidate = new Registration(task, group);
                    Registration current = result.get(mapping.getDevice().getId());
                    if (current == null || candidate.active() && !current.active()) {
                        result.put(mapping.getDevice().getId(), candidate);
                    }
                }
            }
        }
        return result;
    }

    private Map<Integer, List<DeviceModelSnmpPoint>> pointsByModel(List<Device> devices) {
        Set<Integer> modelIds = new LinkedHashSet<>();
        for (Device device : devices) modelIds.add(device.getDeviceModel().getId());
        Map<Integer, List<DeviceModelSnmpPoint>> result = new HashMap<>();
        for (DeviceModelSnmpPoint point : modelPointRepository.findAllEnabledByDeviceModelIds(modelIds)) {
            result.computeIfAbsent(point.getModelProtocol().getDeviceModel().getId(), ignored -> new ArrayList<>()).add(point);
        }
        return result;
    }

    private Map<Integer, List<LastPoint>> latestPoints(
            List<Device> devices,
            Map<Integer, Registration> registrations,
            Map<Integer, List<DeviceModelSnmpPoint>> pointsByModelId,
            Duration lookback
    ) {
        List<Integer> deviceIds = new ArrayList<>();
        Set<String> pointNames = new LinkedHashSet<>();
        for (Device device : devices) {
            if (!device.isEnabled()) continue;
            Registration registration = registrations.get(device.getId());
            if (registration == null || !registration.active()) continue;
            List<DeviceModelSnmpPoint> points = pointsByModelId.getOrDefault(device.getDeviceModel().getId(), List.of());
            if (points.isEmpty()) continue;
            deviceIds.add(device.getId());
            for (DeviceModelSnmpPoint point : points) pointNames.add(point.getName());
        }
        if (deviceIds.isEmpty() || pointNames.isEmpty()) return Map.of();
        Map<Integer, List<LastPoint>> result = new HashMap<>();
        for (LastPoint point : pointQuery.findLast(deviceIds, List.copyOf(pointNames), lookback)) {
            result.computeIfAbsent(point.deviceId(), ignored -> new ArrayList<>()).add(point);
        }
        return result;
    }

    private CollectionDeviceStatusResponse toResponse(
            Device device,
            Registration registration,
            List<DeviceModelSnmpPoint> expectedPoints,
            List<LastPoint> latestPoints,
            Instant now
    ) {
        Map<String, DeviceModelSnmpPoint> pointByName = new LinkedHashMap<>();
        for (DeviceModelSnmpPoint point : expectedPoints) pointByName.putIfAbsent(point.getName(), point);
        List<LastPoint> devicePoints = latestPoints.stream()
                .filter(point -> pointByName.containsKey(point.pointName()))
                .toList();
        String status;
        String message;
        Long intervalSeconds = registration == null ? null : cronIntervalSeconds(registration.group().getCronExpression());
        Instant latest = devicePoints.stream().map(LastPoint::time).max(Comparator.naturalOrder()).orElse(null);
        Long ageSeconds = latest == null ? null : Math.max(Duration.between(latest, now).getSeconds(), 0L);
        if (!device.isEnabled()) {
            status = "DISABLED";
            message = "장비가 비활성 상태입니다.";
        } else if (registration == null) {
            status = "UNREGISTERED";
            message = "수집 작업 그룹에 등록되지 않았습니다.";
        } else if (!registration.active()) {
            status = "STOPPED";
            message = !registration.task().isActive() ? "수집 작업이 중지되었습니다." : "수집 그룹이 중지되었습니다.";
        } else if (expectedPoints.isEmpty()) {
            status = "NO_POINTS";
            message = "모델에 활성 측정항목이 없습니다.";
        } else if (latest == null) {
            status = "MISSING";
            message = "조회 기간 안에 InfluxDB 저장값이 없습니다.";
        } else if (ageSeconds > freshnessSeconds(intervalSeconds)) {
            status = "STALE";
            message = "수집 주기 대비 최신 데이터가 지연되었습니다.";
        } else {
            status = "NORMAL";
            message = "수집 데이터가 정상 범위입니다.";
        }
        List<CollectionDeviceStatusResponse.LatestValue> values = devicePoints.stream()
                .sorted(Comparator.comparing(LastPoint::time).reversed())
                .limit(3)
                .map(point -> new CollectionDeviceStatusResponse.LatestValue(point.pointName(), point.value(),
                        pointByName.get(point.pointName()).getUnit(), point.time()))
                .toList();
        return new CollectionDeviceStatusResponse(
                device.getId(), device.getName(), device.getDeviceModel().getId(), device.getDeviceModel().getName(), device.getDeviceModel().getManufacturer(),
                device.getLocationNode().getName(), device.getLocationNode().getCode(), device.isEnabled(),
                status, message,
                registration == null ? null : registration.task().getId(), registration == null ? null : registration.task().getName(),
                registration == null ? null : registration.group().getId(), registration == null ? null : registration.group().getName(),
                registration == null ? null : registration.group().getCronExpression(),
                registration == null ? null : registration.group().getCollectorJobId(), intervalSeconds,
                latest, ageSeconds, expectedPoints.size(), devicePoints.size(), values);
    }

    private static long freshnessSeconds(Long intervalSeconds) {
        long interval = intervalSeconds == null ? 900L : intervalSeconds;
        return Math.max(interval * 2, 300L);
    }

    private static Long cronIntervalSeconds(String expression) {
        try {
            CronExpression cron = CronExpression.parse(expression);
            ZonedDateTime first = cron.next(ZonedDateTime.now(ZoneOffset.UTC));
            ZonedDateTime second = first == null ? null : cron.next(first);
            return first == null || second == null ? null : Duration.between(first, second).getSeconds();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static int resolveLookbackHours(Integer value) {
        int hours = value == null ? DEFAULT_LOOKBACK_HOURS : value;
        if (hours < 1 || hours > MAX_LOOKBACK_HOURS) {
            throw new IllegalArgumentException("lookbackHours must be between 1 and " + MAX_LOOKBACK_HOURS);
        }
        return hours;
    }

    private static CollectionStatusResponse.CollectionStatusSummary summarize(List<CollectionDeviceStatusResponse> rows) {
        int normal = 0, stale = 0, missing = 0, stopped = 0, unregistered = 0, disabled = 0;
        for (CollectionDeviceStatusResponse row : rows) {
            switch (row.status()) {
                case "NORMAL" -> normal++;
                case "STALE" -> stale++;
                case "MISSING", "NO_POINTS" -> missing++;
                case "STOPPED" -> stopped++;
                case "UNREGISTERED" -> unregistered++;
                case "DISABLED" -> disabled++;
                default -> { }
            }
        }
        return new CollectionStatusResponse.CollectionStatusSummary(rows.size(), normal, stale, missing, stopped, unregistered, disabled);
    }

    private record Registration(CollectionTask task, CollectionTaskGroup group) {
        boolean active() { return task.isActive() && group.isActive(); }
    }
}
