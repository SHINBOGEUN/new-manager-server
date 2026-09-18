package net.vivans.dcim.module.query.application;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.collectortask.application.CollectionGroupOidSpec;
import net.vivans.dcim.module.collectortask.application.CollectionGroupSpec;
import net.vivans.dcim.module.collectortask.application.CollectionGroupSpecService;
import net.vivans.dcim.module.collectortask.domain.model.CollectionTask;
import net.vivans.dcim.module.collectortask.domain.model.CollectionTaskDevice;
import net.vivans.dcim.module.collectortask.domain.model.CollectionTaskGroup;
import net.vivans.dcim.module.collectortask.domain.repository.CollectionTaskRepository;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import net.vivans.dcim.module.devicemodel.application.DeviceModelPointCatalog;
import net.vivans.dcim.module.query.api.dto.CollectionDeviceStatusResponse;
import net.vivans.dcim.module.query.api.dto.CollectionStatusResponse;
import net.vivans.dcim.module.query.config.CollectionStatusProperties;
import net.vivans.dcim.module.query.domain.LastPoint;
import net.vivans.dcim.module.query.domain.PointQuery;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 장비 등록·수집 작업·Influx 최신 데이터를 한 화면의 운영 상태로 결합한다.
 * <p>
 * 프로토콜별 분기를 두지 않는다. 수집 그룹이 실제로 수집할 point 목록과 실패 사유는
 * {@link CollectionGroupSpecService#generate}가 돌려주는 {@link CollectionGroupSpec}에서,
 * 측정항목 단위는 {@link DeviceModelPointCatalog}에서 가져온다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CollectionStatusQueryService {
    private static final int MAX_DEVICES = 200;
    private static final int DEFAULT_LOOKBACK_HOURS = 168;
    private static final int MAX_LOOKBACK_HOURS = 168;
    private static final String UNSUPPORTED_MARKER = "not supported yet";

    private final DeviceRepository deviceRepository;
    private final CollectionTaskRepository collectionTaskRepository;
    private final CollectionGroupSpecService collectionGroupSpecService;
    private final DeviceModelPointCatalog deviceModelPointCatalog;
    private final CollectionStatusProperties statusProperties;
    private final PointQuery pointQuery;

    public CollectionStatusResponse getStatus(Integer lookbackHours) {
        Instant now = Instant.now();
        int resolvedLookbackHours = resolveLookbackHours(lookbackHours);
        List<Device> devices = deviceRepository.findAll(null, null, null, null, null, null,
                PageRequest.of(0, MAX_DEVICES, org.springframework.data.domain.Sort.by("id"))).getContent();
        Map<Integer, Registration> registrationByDeviceId = registrations();
        Map<Integer, CollectionGroupSpec> specByGroupId = specsByGroup(registrationByDeviceId);
        Map<Integer, Map<String, String>> unitsByModelId = deviceModelPointCatalog.unitsByModelId(modelIds(devices));
        Map<Integer, List<LastPoint>> lastPointsByDeviceId = latestPoints(devices, registrationByDeviceId,
                specByGroupId, Duration.ofHours(resolvedLookbackHours));

        List<CollectionDeviceStatusResponse> rows = new ArrayList<>();
        for (Device device : devices) {
            Registration registration = registrationByDeviceId.get(device.getId());
            CollectionGroupSpec spec = registration == null ? null : specByGroupId.get(registration.group().getId());
            List<LastPoint> latestPoints = lastPointsByDeviceId.getOrDefault(device.getId(), List.of());
            Map<String, String> units = unitsByModelId.getOrDefault(device.getDeviceModel().getId(), Map.of());
            rows.add(toResponse(device, registration, spec, latestPoints, units, now, resolvedLookbackHours));
        }
        rows.sort(Comparator.comparing(CollectionDeviceStatusResponse::status)
                .thenComparing(CollectionDeviceStatusResponse::deviceName));
        return new CollectionStatusResponse(now, summarize(rows), rows);
    }

    private static Set<Integer> modelIds(List<Device> devices) {
        Set<Integer> modelIds = new LinkedHashSet<>();
        for (Device device : devices) modelIds.add(device.getDeviceModel().getId());
        return modelIds;
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

    /** 등록된(활성 여부 무관) 그룹마다 spec을 한 번씩만 생성한다. */
    private Map<Integer, CollectionGroupSpec> specsByGroup(Map<Integer, Registration> registrations) {
        Map<Integer, CollectionGroupSpec> result = new HashMap<>();
        for (Registration registration : registrations.values()) {
            Integer groupId = registration.group().getId();
            result.computeIfAbsent(groupId, ignored -> collectionGroupSpecService.generate(registration.group()));
        }
        return result;
    }

    private Map<Integer, List<LastPoint>> latestPoints(
            List<Device> devices,
            Map<Integer, Registration> registrations,
            Map<Integer, CollectionGroupSpec> specByGroupId,
            Duration lookback
    ) {
        List<Integer> deviceIds = new ArrayList<>();
        Set<String> pointNames = new LinkedHashSet<>();
        for (Device device : devices) {
            if (!device.isEnabled()) continue;
            Registration registration = registrations.get(device.getId());
            if (registration == null || !registration.active()) continue;
            CollectionGroupSpec spec = specByGroupId.get(registration.group().getId());
            if (spec == null || spec.oids().isEmpty()) continue;
            deviceIds.add(device.getId());
            for (CollectionGroupOidSpec oid : spec.oids()) pointNames.add(oid.name());
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
            CollectionGroupSpec spec,
            List<LastPoint> latestPoints,
            Map<String, String> unitByPointName,
            Instant now,
            int lookbackHours
    ) {
        Set<String> expectedPointNames = new LinkedHashSet<>();
        if (spec != null) {
            for (CollectionGroupOidSpec oid : spec.oids()) expectedPointNames.add(oid.name());
        }
        List<LastPoint> devicePoints = latestPoints.stream()
                .filter(point -> expectedPointNames.contains(point.pointName()))
                .toList();

        String protocol = registration == null ? null : registration.task().getScriptType().getCode();
        Long intervalSeconds = registration == null ? null : cronIntervalSeconds(registration.group().getCronExpression());
        long interval = intervalSeconds == null ? statusProperties.getDefaultIntervalSeconds() : intervalSeconds;
        long staleAfterSeconds = staleAfterSeconds(interval);
        long failureAfterSeconds = failureAfterSeconds(interval, staleAfterSeconds);
        Instant latest = devicePoints.stream().map(LastPoint::time).max(Comparator.naturalOrder()).orElse(null);
        Long ageSeconds = latest == null ? null : Math.max(Duration.between(latest, now).getSeconds(), 0L);

        // 그룹 spec의 skipped 사유 중 '이 장비' 항목만 골라낸다. 그룹 내 다른 장비의 사유가
        // 이 장비 상태에 잘못 섞여 들어가지 않게 하기 위함이다.
        Optional<String> deviceSkipReason = spec == null ? Optional.empty()
                : spec.skipped().stream().filter(reason -> reason.startsWith("device:" + device.getId() + " ")).findFirst();
        boolean modelMismatch = deviceSkipReason.filter(reason -> reason.contains(CollectionGroupSpecService.MODEL_MISMATCH_MARKER)).isPresent();
        Integer expectedModelId = registration == null ? null : registration.task().getDeviceModel().getId();
        String expectedModelName = registration == null ? null : registration.task().getDeviceModel().getName();

        String status;
        String message;
        String technicalDetail = null;

        if (!device.isEnabled()) {
            status = "DISABLED";
            message = "비활성화됨";
            technicalDetail = "장비가 비활성 상태입니다.";
        } else if (registration == null) {
            status = "UNREGISTERED";
            message = "필수 설정 누락";
            technicalDetail = "장비가 어떤 수집 작업 그룹에도 등록되어 있지 않습니다.";
        } else if (!registration.active()) {
            status = "STOPPED";
            message = "비활성화됨";
            technicalDetail = !registration.task().isActive() ? "수집 작업이 중지되었습니다." : "수집 그룹이 중지되었습니다.";
        } else if (spec == null || spec.oids().isEmpty()) {
            // 그룹에 수집 대상 포인트 자체가 없는 경우에만 설정 누락으로 즉시 판정한다.
            status = "NO_POINTS";
            boolean unsupported = spec != null && spec.skipped().stream().anyMatch(reason -> reason.contains(UNSUPPORTED_MARKER));
            message = unsupported ? "미지원 프로토콜" : "필수 설정 누락";
            technicalDetail = spec == null
                    ? "수집 spec을 생성하지 못했습니다."
                    : (spec.skipped().isEmpty() ? "수집 대상 포인트가 없습니다." : String.join(" / ", spec.skipped()));
        } else if (modelMismatch) {
            // 장비 모델이 수집 작업 모델과 달라 애초에 수집 대상이 아닌 경우. 프로토콜 설정이
            // 있고 없고와 무관하므로 '설정 누락'과는 분리해서 보여준다. 과거에 남은 값이 있어도
            // 지금은 이 그룹 기준으로 유효한 데이터가 아니므로 신선도 판정보다 우선한다.
            status = "MODEL_MISMATCH";
            message = "모델 불일치";
            technicalDetail = deviceSkipReason.orElse("장비 모델이 수집 작업 모델과 다릅니다.");
        } else if (latest != null) {
            // 실제 최근 수집값이 있으면, 그룹 내 다른 장비 때문에 생긴 설정 경고(skipped)보다
            // 이 장비의 실측 데이터 신선도를 우선한다.
            if (ageSeconds > failureAfterSeconds) {
                // 과거 저장값이 남아 있어도 실패 기준을 넘기면 현재는 수집되지 않는 것으로 본다.
                status = "MISSING";
                message = "응답 없음";
                technicalDetail = "마지막 저장값이 " + ageSeconds + "초 전입니다."
                        + " 실패 기준 " + failureAfterSeconds + "초(수집 주기 " + interval + "초)를 넘겼습니다.";
            } else if (ageSeconds > staleAfterSeconds) {
                status = "STALE";
                message = "응답 지연";
                technicalDetail = "마지막 저장값이 " + ageSeconds + "초 전입니다."
                        + " 허용 지연 " + staleAfterSeconds + "초(수집 주기 " + interval + "초)를 넘겼습니다.";
            } else {
                status = "NORMAL";
                message = "정상";
            }
        } else if (!spec.skipped().isEmpty()) {
            // 데이터가 없고, 그룹 spec에 설정 경고가 남아있는 경우에만 설정 누락으로 안내한다.
            status = "NO_POINTS";
            boolean unsupported = spec.skipped().stream().anyMatch(reason -> reason.contains(UNSUPPORTED_MARKER));
            message = unsupported ? "미지원 프로토콜" : "필수 설정 누락";
            technicalDetail = String.join(" / ", spec.skipped());
        } else {
            status = "MISSING";
            message = "응답 없음";
            technicalDetail = "최근 " + lookbackHours + "시간 안에 저장된 수집값이 없습니다."
                    + " (수집 주기 " + interval + "초)";
        }

        List<CollectionDeviceStatusResponse.LatestValue> values = devicePoints.stream()
                .sorted(Comparator.comparing(LastPoint::time).reversed())
                .limit(3)
                .map(point -> new CollectionDeviceStatusResponse.LatestValue(
                        point.pointName(), point.value(), unitByPointName.get(point.pointName()), point.time()))
                .toList();

        return new CollectionDeviceStatusResponse(
                device.getId(), device.getName(), device.getDeviceModel().getId(), device.getDeviceModel().getName(), device.getDeviceModel().getManufacturer(),
                expectedModelId, expectedModelName,
                device.getLocationNode().getName(), device.getLocationNode().getCode(), device.isEnabled(),
                protocol,
                status, message, technicalDetail,
                registration == null ? null : registration.task().getId(), registration == null ? null : registration.task().getName(),
                registration == null ? null : registration.group().getId(), registration == null ? null : registration.group().getName(),
                registration == null ? null : registration.group().getCronExpression(),
                registration == null ? null : registration.group().getCollectorJobId(), intervalSeconds,
                latest, ageSeconds, expectedPointNames.size(), devicePoints.size(), values);
    }

    private long staleAfterSeconds(long intervalSeconds) {
        return Math.max(intervalSeconds * statusProperties.getStaleMultiplier(), statusProperties.getMinStaleSeconds());
    }

    private long failureAfterSeconds(long intervalSeconds, long staleAfterSeconds) {
        long byInterval = Math.max(
                intervalSeconds * statusProperties.getFailureMultiplier(),
                statusProperties.getMinFailureSeconds()
        );
        return Math.max(byInterval, staleAfterSeconds);
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
                case "MISSING", "NO_POINTS", "MODEL_MISMATCH" -> missing++;
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
