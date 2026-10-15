package net.vivans.dcim.module.query.application;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.api.dto.DevicePageOptionsResponse;
import net.vivans.dcim.module.device.api.dto.DevicePageOptionsResponse.DeviceOption;
import net.vivans.dcim.module.device.api.dto.DevicePageOptionsResponse.PointOption;
import net.vivans.dcim.module.device.application.DevicePageOptionsQueryService;
import net.vivans.dcim.module.query.api.dto.AnalysisDeviceResponse;
import net.vivans.dcim.module.query.api.dto.AnalysisDeviceResponse.Field;
import net.vivans.dcim.module.query.api.dto.AnalysisDeviceResponse.RawValue;
import net.vivans.dcim.module.query.api.dto.AnalysisDeviceResponse.SensorGroup;
import net.vivans.dcim.module.query.api.dto.AnalysisDeviceResponse.StatsValue;
import net.vivans.dcim.module.query.api.dto.AnalysisDeviceResponse.UnitGroup;
import net.vivans.dcim.module.query.api.dto.AnalysisMeasurementsRequest;
import net.vivans.dcim.module.query.domain.PointQuery;
import net.vivans.dcim.module.query.domain.SeriesPoint;
import net.vivans.dcim.module.query.domain.StatsSeriesPoint;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalysisQueryService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final int MAX_TARGETS = 100;
    private static final int MAX_SELECTED_POINTS = 500;

    private final DevicePageOptionsQueryService pageOptionsQueryService;
    private final PointQuery pointQuery;

    public record ComponentOption(Integer deviceId, String componentType, String componentIndex) {}

    public List<ComponentOption> getComponents(AnalysisMeasurementsRequest request) {
        validateRange(request.startDate(), request.endDate());
        if (request.targets().size() > MAX_TARGETS) throw new IllegalArgumentException("too many targets");
        Map<Integer, DeviceOption> devices = new HashMap<>();
        pageOptionsQueryService.getOptions("ANALYSIS").devices().forEach(device -> devices.put(device.deviceId(), device));
        var selections = resolveSelections(request.targets(), devices);
        if (selections.stream().mapToInt(s -> s.points().size()).sum() > MAX_SELECTED_POINTS) throw new IllegalArgumentException("too many points");
        var start = request.startDate().atStartOfDay(SEOUL).toInstant();
        var end = request.endDate().plusDays(1).atStartOfDay(SEOUL).toInstant();
        var ids = selections.stream().map(s -> s.device().deviceId()).toList();
        var names = selections.stream().flatMap(s -> s.points().stream()).filter(p -> "COMPONENT".equals(p.componentScope()))
                .map(PointOption::pointName).distinct().toList();
        if (names.isEmpty()) return List.of();
        return pointQuery.findStatsSeries(ids, names, start, end, "1d").stream()
                .filter(value -> value.componentType() != null && value.componentIndex() != null)
                .filter(value -> selections.stream().anyMatch(s -> s.device().deviceId().equals(value.deviceId())
                        && s.points().stream().anyMatch(p -> p.pointName().equals(value.pointName())
                        && "COMPONENT".equals(p.componentScope()) && java.util.Objects.equals(p.componentType(), value.componentType()))))
                .map(value -> new ComponentOption(value.deviceId(), value.componentType(), value.componentIndex()))
                .distinct().sorted(Comparator.comparing(ComponentOption::deviceId).thenComparing(ComponentOption::componentType)
                        .thenComparingInt(c -> c.componentIndex().length()).thenComparing(ComponentOption::componentIndex)).toList();
    }

    public List<AnalysisDeviceResponse> getMeasurements(AnalysisMeasurementsRequest request) {
        validateRange(request.startDate(), request.endDate());
        if (request.targets().size() > MAX_TARGETS) {
            throw new IllegalArgumentException("targets must not exceed " + MAX_TARGETS);
        }

        DevicePageOptionsResponse options = pageOptionsQueryService.getOptions("ANALYSIS");
        Map<Integer, DeviceOption> devicesById = new HashMap<>();
        for (DeviceOption device : options.devices()) {
            devicesById.put(device.deviceId(), device);
        }

        List<TargetSelection> selections = resolveSelections(request.targets(), devicesById);
        int selectedPointCount = selections.stream().mapToInt(selection -> selection.points().size()).sum();
        if (selectedPointCount > MAX_SELECTED_POINTS) {
            throw new IllegalArgumentException("selected points must not exceed " + MAX_SELECTED_POINTS);
        }

        long days = ChronoUnit.DAYS.between(request.startDate(), request.endDate()) + 1;
        AnalysisAggregation aggregation = AnalysisAggregation.forInclusiveDays(days);
        var start = request.startDate().atStartOfDay(SEOUL).toInstant();
        var endExclusive = request.endDate().plusDays(1).atStartOfDay(SEOUL).toInstant();
        List<Integer> deviceIds = selections.stream()
                .map(selection -> selection.device().deviceId()).toList();
        List<String> pointNames = selections.stream()
                .flatMap(selection -> selection.points().stream())
                .map(PointOption::pointName).distinct().toList();

        Map<SeriesIdentity, List<SeriesPoint>> rawBySeries = new HashMap<>();
        Map<SeriesIdentity, List<StatsSeriesPoint>> statsBySeries = new HashMap<>();
        if ("raw".equals(aggregation.label())) {
            for (SeriesPoint point : pointQuery.findRawSeries(deviceIds, pointNames, start, endExclusive)) {
                rawBySeries.computeIfAbsent(new SeriesIdentity(point.deviceId(), point.pointName(), point.componentType(), point.componentIndex()),
                        ignored -> new ArrayList<>()).add(point);
            }
        } else {
            for (StatsSeriesPoint point : pointQuery.findStatsSeries(
                    deviceIds, pointNames, start, endExclusive, aggregation.window())) {
                statsBySeries.computeIfAbsent(new SeriesIdentity(point.deviceId(), point.pointName(), point.componentType(), point.componentIndex()),
                        ignored -> new ArrayList<>()).add(point);
            }
        }

        return selections.stream()
                .map(selection -> toResponse(selection, request, aggregation, rawBySeries, statsBySeries))
                .toList();
    }

    private static List<TargetSelection> resolveSelections(
            List<AnalysisMeasurementsRequest.Target> requestedTargets,
            Map<Integer, DeviceOption> devicesById
    ) {
        Set<Integer> seenDevices = new LinkedHashSet<>();
        List<TargetSelection> selections = new ArrayList<>(requestedTargets.size());
        for (AnalysisMeasurementsRequest.Target target : requestedTargets) {
            if (!seenDevices.add(target.deviceId())) {
                throw new IllegalArgumentException("duplicate target deviceId: " + target.deviceId());
            }
            DeviceOption device = devicesById.get(target.deviceId());
            if (device == null) {
                throw new IllegalArgumentException(
                        "device is not registered for ANALYSIS page: " + target.deviceId());
            }

            Map<String, List<PointOption>> pointsByName = new LinkedHashMap<>();
            for (PointOption point : device.points()) {
                pointsByName.computeIfAbsent(point.pointName(), ignored -> new ArrayList<>()).add(point);
            }
            LinkedHashSet<String> requestedNames = new LinkedHashSet<>(target.pointNames());
            List<PointOption> selectedPoints = new ArrayList<>(requestedNames.size());
            for (String pointName : requestedNames) {
                List<PointOption> candidates = pointsByName.get(pointName);
                if (candidates == null || candidates.isEmpty()) {
                    throw new IllegalArgumentException("point is not available for ANALYSIS: deviceId="
                            + target.deviceId() + ", pointName=" + pointName);
                }
                if (candidates.size() > 1) {
                    throw new IllegalArgumentException("pointName is ambiguous for ANALYSIS: deviceId="
                            + target.deviceId() + ", pointName=" + pointName);
                }
                selectedPoints.add(candidates.get(0));
            }
            selectedPoints.sort(Comparator.comparingInt(PointOption::sortOrder)
                    .thenComparing(PointOption::pointName));
            if (target.componentIndices() != null && (target.componentIndices().size() > 128
                    || target.componentIndices().stream().anyMatch(index -> index == null || !index.matches("0|[1-9][0-9]{0,9}")))) {
                throw new IllegalArgumentException("componentIndices must contain at most 128 non-negative GPU indices");
            }
            if (target.componentSelections() != null) {
                if (target.componentSelections().size() > 500) throw new IllegalArgumentException("too many component selections");
                for (var component : target.componentSelections()) {
                    if (component == null || component.componentIndex() == null || !component.componentIndex().matches("0|[1-9][0-9]{0,9}")
                            || selectedPoints.stream().noneMatch(p -> p.pointName().equals(component.pointName())
                            && "COMPONENT".equals(p.componentScope()) && java.util.Objects.equals(p.componentType(), component.componentType()))) {
                        throw new IllegalArgumentException("component selection is not available for ANALYSIS");
                    }
                }
            }
            selections.add(new TargetSelection(device, List.copyOf(selectedPoints),
                    target.componentIndices() == null ? null : Set.copyOf(target.componentIndices()), target.componentSelections()));
        }
        return List.copyOf(selections);
    }

    private static AnalysisDeviceResponse toResponse(
            TargetSelection selection,
            AnalysisMeasurementsRequest request,
            AnalysisAggregation aggregation,
            Map<SeriesIdentity, List<SeriesPoint>> rawBySeries,
            Map<SeriesIdentity, List<StatsSeriesPoint>> statsBySeries
    ) {
        LinkedHashMap<String, LinkedHashMap<String, List<Field>>> fieldsByCategoryAndUnit = new LinkedHashMap<>();
        for (PointOption point : selection.points()) {
            Set<SeriesIdentity> available = new HashSet<>();
            available.addAll(rawBySeries.keySet());
            available.addAll(statsBySeries.keySet());
            boolean component = "COMPONENT".equals(point.componentScope());
            List<SeriesIdentity> keys = available.stream()
                    .filter(key -> key.deviceId().equals(selection.device().deviceId()) && key.pointName().equals(point.pointName()))
                    .filter(key -> component
                            ? java.util.Objects.equals(point.componentType(), key.componentType()) && key.componentIndex() != null
                            : key.componentType() == null && key.componentIndex() == null)
                    .filter(key -> !component || selection.componentIndices() == null || selection.componentIndices().contains(key.componentIndex()))
                    .filter(key -> !component || selection.componentSelections() == null || selection.componentSelections().stream()
                            .anyMatch(c -> c.pointName().equals(key.pointName()) && c.componentType().equals(key.componentType()) && c.componentIndex().equals(key.componentIndex())))
                    .sorted(Comparator.comparingInt((SeriesIdentity key) -> key.componentIndex() == null ? 0 : key.componentIndex().length())
                            .thenComparing(key -> key.componentIndex() == null ? "" : key.componentIndex()))
                    .toList();
            if (!component && keys.isEmpty()) keys = List.of(new SeriesIdentity(selection.device().deviceId(), point.pointName(), null, null));
            for (SeriesIdentity key : keys) {
                List<RawValue> raws = null;
                List<StatsValue> stats = null;
                if ("raw".equals(aggregation.label())) {
                    raws = rawBySeries.getOrDefault(key, List.of()).stream()
                            .sorted(Comparator.comparing(SeriesPoint::time))
                            .map(value -> new RawValue(value.time(), value.value())).toList();
                } else {
                    stats = statsBySeries.getOrDefault(key, List.of()).stream()
                            .sorted(Comparator.comparing(StatsSeriesPoint::time))
                            .map(value -> new StatsValue(value.time(), value.min(), value.max(), value.avg())).toList();
                }
                String displayName = component ? key.componentType() + " " + key.componentIndex() + " · " + point.pointName() : point.pointName();
                Field field = new Field(point.pointName(), displayName, raws, stats, key.componentType(), key.componentIndex());
                fieldsByCategoryAndUnit.computeIfAbsent(point.categoryName(), ignored -> new LinkedHashMap<>())
                        .computeIfAbsent(point.unit(), ignored -> new ArrayList<>()).add(field);
            }
        }

        List<SensorGroup> sensors = fieldsByCategoryAndUnit.entrySet().stream()
                .map(category -> new SensorGroup(category.getKey(), category.getValue().entrySet().stream()
                        .map(unit -> new UnitGroup(unit.getKey(), List.copyOf(unit.getValue())))
                        .toList()))
                .toList();
        DeviceOption device = selection.device();
        return new AnalysisDeviceResponse(device.deviceId(), device.deviceName(), device.type(),
                aggregation.label(), request.startDate(), request.endDate(), sensors);
    }

    private static void validateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("startDate and endDate are required");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must be on or after startDate");
        }
    }

    private record TargetSelection(DeviceOption device, List<PointOption> points, Set<String> componentIndices,
                                   List<AnalysisMeasurementsRequest.ComponentSelection> componentSelections) {
    }

    private record SeriesIdentity(Integer deviceId, String pointName, String componentType, String componentIndex) {
    }
}
