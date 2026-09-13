package net.vivans.dcim.module.query.application;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.model.PageWidgetChartRangePreset;
import net.vivans.dcim.module.device.domain.model.PageWidget;
import net.vivans.dcim.module.pue.domain.model.PueDefinitionSourceRole;
import net.vivans.dcim.module.pue.domain.model.PueDefinition;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import net.vivans.dcim.module.device.domain.repository.PageWidgetRepository;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModelSnmpPoint;
import net.vivans.dcim.module.devicemodel.domain.repository.DeviceModelSnmpPointRepository;
import net.vivans.dcim.module.query.api.dto.PueDeviceValueResponse;
import net.vivans.dcim.module.query.api.dto.PueQueryRequest;
import net.vivans.dcim.module.query.api.dto.PueQueryResponse;
import net.vivans.dcim.module.query.api.dto.PueSourceRequest;
import net.vivans.dcim.module.query.api.dto.PueTrendPointResponse;
import net.vivans.dcim.module.query.api.dto.WidgetDataStatusResponse;
import net.vivans.dcim.module.query.domain.LastPoint;
import net.vivans.dcim.module.query.domain.PointQuery;
import net.vivans.dcim.module.query.domain.PueLastPoint;
import net.vivans.dcim.module.query.domain.PueSeriesPoint;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PueQueryService {

    static final int MAX_SOURCES = 200;
    private static final String POWER_DATA_POINT_TYPE = "POWER";
    private static final String ROLE_TOTAL = "total";
    private static final String ROLE_COOLER = "cooler";

    private final DeviceRepository deviceRepository;
    private final DeviceModelSnmpPointRepository deviceModelSnmpPointRepository;
    private final PointQuery pointQuery;
    private final PageWidgetRepository pageWidgetRepository;
    private static final WidgetDataStatusResolver WIDGET_DATA_STATUS_RESOLVER = new WidgetDataStatusResolver();

    public PueQueryResponse getPue(Integer widgetId) {
        PageWidget widget = pageWidgetRepository.findById(widgetId)
                .orElseThrow(() -> new EntityNotFoundException("PageWidget not found: " + widgetId));
        if (widget.getQueryKind() != net.vivans.dcim.module.device.domain.model.PageWidgetQueryKind.pue) {
            throw new IllegalArgumentException("queryKind must be pue");
        }
        if (!widget.isEnabled()) throw new IllegalArgumentException("widget is disabled");
        if (widget.getPueDefinitionId() == null) {
            throw new IllegalArgumentException("PUE widget has no definition");
        }
        PueDefinition definition = widget.getPue().getPueDefinition();
        List<PueSourceRequest> total = definition.resolvedSources().stream()
                .filter(source -> source.role() == PueDefinitionSourceRole.total)
                .map(source -> new PueSourceRequest(source.device().getId(), source.pointName()))
                .toList();
        List<PueSourceRequest> cooler = definition.resolvedSources().stream()
                .filter(source -> source.role() == PueDefinitionSourceRole.cooler)
                .map(source -> new PueSourceRequest(source.device().getId(), source.pointName()))
                .toList();

        List<RequestedSource> sources = new ArrayList<>();
        addResolvedSources(sources, total, ROLE_TOTAL);
        addResolvedSources(sources, cooler, ROLE_COOLER);
        String unit = validatePowerPointsAndResolveUnit(sources);

        PageWidgetChartRangePreset rangePreset = resolveRangePreset(
                widget.getPueRangePreset() == null ? null : widget.getPueRangePreset().name());
        QueryRanges.Range range = QueryRanges.resolve(rangePreset);
        Duration lookback = Duration.between(range.start(), range.end());
        Optional<PueLastPoint> last = pointQuery.findLastPue(definition.getId(), lookback);
        PueLastPoint point = last.orElse(null);
        boolean stale = point != null && widget.getPueFreshnessMinutes() != null
                && point.time().isBefore(Instant.now().minusSeconds(
                widget.getPueFreshnessMinutes().longValue() * 60));
        boolean complete = point != null && !stale;
        List<Integer> sourceDeviceIds = sources.stream()
                .map(source -> source.device().getId())
                .distinct()
                .toList();
        WidgetDataStatusResponse dataStatus = WIDGET_DATA_STATUS_RESOLVER.resolve(
                java.util.Collections.singletonList(point == null ? null : point.time()), widget.getPueFreshnessMinutes());

        return new PueQueryResponse(
                complete ? QueryValues.round4(point.value()) : null,
                complete ? QueryValues.round2(point.totalPower()) : null,
                complete ? QueryValues.round2(point.coolerPower()) : null,
                unit,
                rangePreset.name(),
                range.start(),
                range.end(),
                complete,
                point == null ? "MISSING_DATA" : stale ? "STALE_DATA" : "OK",
                complete ? List.of() : sourceDeviceIds,
                stale ? sourceDeviceIds : List.of(),
                List.of(),
                List.of(),
                dataStatus
        );
    }

    public PueQueryResponse getPue(Integer widgetId, String rangePresetOverride, String windowOverride) {
        PueQueryResponse latest = getPue(widgetId);
        if ((rangePresetOverride == null || rangePresetOverride.isBlank())
                && (windowOverride == null || windowOverride.isBlank())) {
            return latest;
        }
        PageWidget widget = pageWidgetRepository.findById(widgetId)
                .orElseThrow(() -> new EntityNotFoundException("PageWidget not found: " + widgetId));
        PageWidgetChartRangePreset preset = resolveRangePreset(rangePresetOverride);
        String window = resolveTrendWindow(windowOverride);
        QueryRanges.Range range = QueryRanges.resolve(preset);
        List<PueTrendPointResponse> trend = pointQuery.findPueSeries(
                        widget.getPueDefinitionId(), range.start(), range.end(), window)
                .stream()
                .map(this::toTrendPoint)
                .toList();
        return new PueQueryResponse(
                latest.value(), latest.totalPower(), latest.coolerPower(), latest.unit(),
                preset.name(), range.start(), range.end(), latest.complete(), latest.calculationStatus(),
                latest.missingDeviceIds(), latest.staleDeviceIds(), latest.devices(), trend, latest.dataStatus()
        );
    }

    public PueQueryResponse getPue(PueQueryRequest request) {
        return getPue(request, null);
    }

    private PueQueryResponse getPue(PueQueryRequest request, Integer freshnessMinutes) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        List<PueSourceRequest> totalSources = requireSources(request.totalSources(), "totalSources");
        List<PueSourceRequest> coolerSources = requireSources(request.coolerSources(), "coolerSources");
        if (totalSources.size() + coolerSources.size() > MAX_SOURCES) {
            throw new IllegalArgumentException("PUE supports at most " + MAX_SOURCES + " sources");
        }

        validateUniqueDevices(totalSources, "totalSources");
        validateUniqueDevices(coolerSources, "coolerSources");
        validateRolesDoNotOverlap(totalSources, coolerSources);

        List<RequestedSource> sources = new ArrayList<>();
        addResolvedSources(sources, totalSources, ROLE_TOTAL);
        addResolvedSources(sources, coolerSources, ROLE_COOLER);
        String unit = validatePowerPointsAndResolveUnit(sources);

        PageWidgetChartRangePreset rangePreset = resolveRangePreset(request.rangePreset());
        QueryRanges.Range range = QueryRanges.resolve(rangePreset);
        List<Integer> deviceIds = sources.stream().map(source -> source.device().getId()).toList();
        List<String> pointNames = sources.stream()
                .map(RequestedSource::pointName)
                .distinct()
                .toList();
        Map<String, LastPoint> valueBySource = indexLatestPoints(pointQuery.findLastInRange(
                deviceIds, pointNames, range.start(), range.end()));

        List<PueDeviceValueResponse> rows = new ArrayList<>();
        List<Integer> missingDeviceIds = new ArrayList<>();
        List<Integer> staleDeviceIds = new ArrayList<>();
        double totalSum = 0;
        double coolerSum = 0;
        boolean totalComplete = true;
        boolean coolerComplete = true;

        for (RequestedSource source : sources) {
            LastPoint point = valueBySource.get(key(source.device().getId(), source.pointName()));
            boolean stale = point != null && freshnessMinutes != null
                    && point.time().isBefore(Instant.now().minusSeconds(freshnessMinutes.longValue() * 60));
            if (point == null || stale) {
                missingDeviceIds.add(source.device().getId());
                if (stale) staleDeviceIds.add(source.device().getId());
                if (ROLE_TOTAL.equals(source.role())) {
                    totalComplete = false;
                } else {
                    coolerComplete = false;
                }
            } else if (ROLE_TOTAL.equals(source.role())) {
                totalSum += point.value();
            } else {
                coolerSum += point.value();
            }
            rows.add(new PueDeviceValueResponse(
                    source.device().getId(),
                    source.device().getName(),
                    source.role(),
                    source.pointName(),
                    point == null || stale ? null : QueryValues.round2(point.value()),
                    source.unit(),
                    point == null ? null : point.time()
            ));
        }

        boolean complete = totalComplete && coolerComplete;
        Double totalPower = totalComplete ? totalSum : null;
        Double coolerPower = coolerComplete ? coolerSum : null;
        String status;
        Double pue = null;
        if (!complete) {
            status = "MISSING_DATA";
        } else if (Double.compare(coolerSum, 0.0d) == 0) {
            status = "ZERO_COOLER_POWER";
        } else {
            status = "OK";
            pue = totalSum / coolerSum;
        }

        return new PueQueryResponse(
                QueryValues.round4(pue),
                QueryValues.round2(totalPower),
                QueryValues.round2(coolerPower),
                unit,
                rangePreset.name(),
                range.start(),
                range.end(),
                complete,
                status,
                List.copyOf(missingDeviceIds),
                List.copyOf(staleDeviceIds),
                List.copyOf(rows),
                List.of(),
                null
        );
    }

    private PueTrendPointResponse toTrendPoint(PueSeriesPoint point) {
        return new PueTrendPointResponse(
                point.time(), QueryValues.round4(point.value()),
                QueryValues.round2(point.totalPower()), QueryValues.round2(point.coolerPower())
        );
    }

    private static String resolveTrendWindow(String raw) {
        String window = raw == null || raw.isBlank() ? "15m" : raw.trim();
        if (!Set.of("1m", "5m", "15m", "1h", "1d").contains(window)) {
            throw new IllegalArgumentException("window must be 1m, 5m, 15m, 1h, or 1d");
        }
        return window;
    }

    private void addResolvedSources(
            List<RequestedSource> target,
            List<PueSourceRequest> requests,
            String role
    ) {
        for (PueSourceRequest request : requests) {
            if (request == null || request.deviceId() == null || request.deviceId() <= 0) {
                throw new IllegalArgumentException(role + " source deviceId must be a positive integer");
            }
            String pointName = normalizePointName(request.pointName(), role);
            Device device = deviceRepository.findById(request.deviceId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Device not found: " + request.deviceId()));
            if (!device.isEnabled()) {
                throw new IllegalArgumentException("Device is disabled: " + request.deviceId());
            }
            target.add(new RequestedSource(device, pointName, role, null));
        }
    }

    private String validatePowerPointsAndResolveUnit(List<RequestedSource> sources) {
        Set<Integer> modelIds = new LinkedHashSet<>();
        for (RequestedSource source : sources) {
            modelIds.add(source.device().getDeviceModel().getId());
        }
        List<DeviceModelSnmpPoint> points = deviceModelSnmpPointRepository
                .findAllEnabledByDeviceModelIds(modelIds);
        Map<String, List<DeviceModelSnmpPoint>> pointsByModelAndName = new HashMap<>();
        for (DeviceModelSnmpPoint point : points) {
            Integer modelId = point.getModelProtocol().getDeviceModel().getId();
            pointsByModelAndName.computeIfAbsent(
                    key(modelId, point.getName()), ignored -> new ArrayList<>()).add(point);
        }

        String commonUnit = null;
        for (int index = 0; index < sources.size(); index++) {
            RequestedSource source = sources.get(index);
            Integer modelId = source.device().getDeviceModel().getId();
            List<DeviceModelSnmpPoint> matches = pointsByModelAndName.getOrDefault(
                    key(modelId, source.pointName()), List.of());
            if (matches.isEmpty()) {
                throw new IllegalArgumentException(
                        "POWER point not found for device " + source.device().getId()
                                + ": " + source.pointName());
            }
            DeviceModelSnmpPoint point = matches.get(0);
            String dataPointType = point.getDataPointType() == null
                    ? null : point.getDataPointType().getCode();
            if (!POWER_DATA_POINT_TYPE.equalsIgnoreCase(dataPointType)) {
                throw new IllegalArgumentException(
                        "PUE source point must have DATA_POINT_TYPE=POWER: device "
                                + source.device().getId() + ", point " + source.pointName());
            }
            String unit = normalizeUnit(point.getUnit(), source);
            if (commonUnit == null) {
                commonUnit = unit;
            } else if (!commonUnit.equalsIgnoreCase(unit)) {
                throw new IllegalArgumentException(
                        "PUE source points must use the same unit: " + commonUnit + ", " + unit);
            }
            sources.set(index, new RequestedSource(
                    source.device(), source.pointName(), source.role(), unit));
        }
        return commonUnit;
    }

    private static List<PueSourceRequest> requireSources(
            List<PueSourceRequest> sources,
            String fieldName
    ) {
        if (sources == null || sources.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return sources;
    }

    private static void validateUniqueDevices(List<PueSourceRequest> sources, String fieldName) {
        Set<Integer> deviceIds = new LinkedHashSet<>();
        for (PueSourceRequest source : sources) {
            if (source != null && source.deviceId() != null && !deviceIds.add(source.deviceId())) {
                throw new IllegalArgumentException(
                        fieldName + " contains duplicate deviceId: " + source.deviceId());
            }
        }
    }

    private static void validateRolesDoNotOverlap(
            List<PueSourceRequest> totalSources,
            List<PueSourceRequest> coolerSources
    ) {
        Set<Integer> totalDeviceIds = new LinkedHashSet<>();
        for (PueSourceRequest source : totalSources) {
            if (source != null && source.deviceId() != null) {
                totalDeviceIds.add(source.deviceId());
            }
        }
        for (PueSourceRequest source : coolerSources) {
            if (source != null && source.deviceId() != null
                    && totalDeviceIds.contains(source.deviceId())) {
                throw new IllegalArgumentException(
                        "deviceId cannot belong to both totalSources and coolerSources: "
                                + source.deviceId());
            }
        }
    }

    private static Map<String, LastPoint> indexLatestPoints(List<LastPoint> points) {
        Map<String, LastPoint> result = new LinkedHashMap<>();
        for (LastPoint point : points) {
            String key = key(point.deviceId(), point.pointName());
            result.merge(key, point, (left, right) -> Comparator
                    .comparing(LastPoint::time)
                    .compare(left, right) >= 0 ? left : right);
        }
        return result;
    }

    private static PageWidgetChartRangePreset resolveRangePreset(String raw) {
        if (raw == null || raw.isBlank()) {
            return PageWidgetChartRangePreset.last_24h;
        }
        return PageWidgetChartRangePreset.from(raw);
    }

    private static String normalizePointName(String pointName, String role) {
        if (pointName == null || pointName.isBlank()) {
            throw new IllegalArgumentException(role + " source pointName is required");
        }
        return pointName.trim();
    }

    private static String normalizeUnit(String unit, RequestedSource source) {
        if (unit == null || unit.isBlank()) {
            throw new IllegalArgumentException(
                    "PUE source point unit is required: device " + source.device().getId()
                            + ", point " + source.pointName());
        }
        return unit.trim();
    }

    private static String key(Integer deviceIdOrModelId, String pointName) {
        return deviceIdOrModelId + "|" + pointName.trim().toUpperCase(Locale.ROOT);
    }

    private record RequestedSource(
            Device device,
            String pointName,
            String role,
            String unit
    ) {
    }
}
