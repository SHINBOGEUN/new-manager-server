package net.vivans.dcim.module.query.application;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.domain.model.PageWidget;
import net.vivans.dcim.module.device.domain.model.PageWidgetPsychrometricSource;
import net.vivans.dcim.module.device.domain.model.PageWidgetPsychrometricSourceRole;
import net.vivans.dcim.module.device.domain.model.PageWidgetQueryKind;
import net.vivans.dcim.module.device.domain.repository.PageWidgetRepository;
import net.vivans.dcim.module.query.api.dto.PsychrometricDataResponse;
import net.vivans.dcim.module.query.api.dto.PsychrometricWidgetResponse;
import net.vivans.dcim.module.query.api.dto.WidgetDataStatusResponse;
import net.vivans.dcim.module.query.domain.LastPoint;
import net.vivans.dcim.module.query.domain.PointQuery;
import net.vivans.dcim.module.query.domain.SeriesPoint;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PsychrometricQueryService {

    private static final int HISTORY_POINT_COUNT = 5;
    private static final Duration LATEST_LOOKBACK = Duration.ofHours(24);

    private final PageWidgetRepository pageWidgetRepository;
    private final PointQuery pointQuery;
    private final WidgetDataStatusResolver widgetDataStatusResolver;

    public PsychrometricWidgetResponse getPsychrometric(Integer widgetId) {
        PageWidget widget = findPsychrometricWidget(widgetId);
        List<PageWidgetPsychrometricSource> sources = new ArrayList<>(widget.getPsychrometric().getSources());
        List<Integer> deviceIds = sources.stream()
                .map(source -> source.getDevice().getId())
                .distinct()
                .toList();
        List<String> pointNames = sources.stream()
                .map(PageWidgetPsychrometricSource::getPointName)
                .distinct()
                .toList();

        Map<SourceKey, PageWidgetPsychrometricSourceRole> roles = new HashMap<>();
        for (PageWidgetPsychrometricSource source : sources) {
            roles.put(new SourceKey(source.getDevice().getId(), source.getPointName()), source.getRole());
        }

        Instant now = Instant.now();
        Instant historyStart = now.truncatedTo(ChronoUnit.HOURS).minus(HISTORY_POINT_COUNT - 1L, ChronoUnit.HOURS);
        List<Sample> samples = averageByTime(
                pointQuery.findSeries(deviceIds, pointNames, historyStart, now, "1h"), roles);
        if (samples.size() > HISTORY_POINT_COUNT) {
            samples = samples.subList(samples.size() - HISTORY_POINT_COUNT, samples.size());
        }

        List<LastPoint> latestPoints = pointQuery.findLast(deviceIds, pointNames, LATEST_LOOKBACK);
        java.util.Optional<Sample> latest = latestAverage(latestPoints, roles);
        if (latest.isPresent() && samples.stream().noneMatch(sample -> sample.time().equals(latest.get().time()))) {
            samples.add(latest.get());
        }

        List<Instant> timeLabels = samples.stream().map(Sample::time).toList();
        List<Double> temperatures = samples.stream().map(Sample::temperature).toList();
        List<Double> humidities = samples.stream().map(Sample::humidity).toList();
        Map<SourceKey, LastPoint> latestBySource = new HashMap<>();
        for (LastPoint point : latestPoints) {
            latestBySource.put(new SourceKey(point.deviceId(), point.pointName()), point);
        }
        List<Instant> collectedTimes = sources.stream()
                .map(source -> latestBySource.get(new SourceKey(source.getDevice().getId(), source.getPointName())))
                .map(point -> point == null ? null : point.time())
                .toList();
        WidgetDataStatusResponse dataStatus = widgetDataStatusResolver
                .resolve(collectedTimes, widget.getDataFreshnessMinutes());
        return new PsychrometricWidgetResponse(
                widget.getId(),
                widget.getName(),
                timeLabels,
                List.of(
                        new PsychrometricDataResponse("TEMP_AVG", temperatures, "°C"),
                        new PsychrometricDataResponse("HUM_AVG", humidities, "%")
                ),
                dataStatus
        );
    }

    private List<Sample> averageByTime(
            List<SeriesPoint> points,
            Map<SourceKey, PageWidgetPsychrometricSourceRole> roles
    ) {
        Map<Instant, Values> valuesByTime = new LinkedHashMap<>();
        for (SeriesPoint point : points) {
            PageWidgetPsychrometricSourceRole role = roles.get(new SourceKey(point.deviceId(), point.pointName()));
            if (role == null || point.time() == null || point.value() == null) {
                continue;
            }
            Values values = valuesByTime.computeIfAbsent(point.time(), ignored -> new Values());
            values.add(role, point.value());
        }
        return valuesByTime.entrySet().stream()
                .map(entry -> entry.getValue().toSample(entry.getKey()))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .sorted(Comparator.comparing(Sample::time))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private java.util.Optional<Sample> latestAverage(
            List<LastPoint> points,
            Map<SourceKey, PageWidgetPsychrometricSourceRole> roles
    ) {
        Values values = new Values();
        Instant latestTime = null;
        for (LastPoint point : points) {
            PageWidgetPsychrometricSourceRole role = roles.get(new SourceKey(point.deviceId(), point.pointName()));
            if (role == null || point.time() == null) {
                continue;
            }
            values.add(role, point.value());
            if (latestTime == null || point.time().isAfter(latestTime)) {
                latestTime = point.time();
            }
        }
        return latestTime == null ? java.util.Optional.empty() : values.toSample(latestTime);
    }

    private PageWidget findPsychrometricWidget(Integer widgetId) {
        PageWidget widget = PageWidgetFinder.findRequired(pageWidgetRepository, widgetId);
        if (widget.getQueryKind() != PageWidgetQueryKind.psychrometric) {
            throw new IllegalArgumentException(
                    "widget queryKind must be psychrometric, but was " + widget.getQueryKind());
        }
        if (!widget.isEnabled()) {
            throw new IllegalArgumentException("widget is disabled");
        }
        if (widget.getPsychrometric() == null || widget.getPsychrometric().getSources().isEmpty()) {
            throw new IllegalArgumentException("psychrometric widget sources are required");
        }
        return widget;
    }

    private record SourceKey(Integer deviceId, String pointName) {
    }

    private record Sample(Instant time, Double temperature, Double humidity) {
    }

    private static final class Values {
        private final List<Double> temperatures = new ArrayList<>();
        private final List<Double> humidities = new ArrayList<>();

        void add(PageWidgetPsychrometricSourceRole role, Double value) {
            if (!Double.isFinite(value)) {
                return;
            }
            if (role == PageWidgetPsychrometricSourceRole.temperature) {
                temperatures.add(value);
            } else if (role == PageWidgetPsychrometricSourceRole.humidity) {
                humidities.add(value);
            }
        }

        java.util.Optional<Sample> toSample(Instant time) {
            if (temperatures.isEmpty() || humidities.isEmpty()) {
                return java.util.Optional.empty();
            }
            return java.util.Optional.of(new Sample(time, average(temperatures), average(humidities)));
        }

        private static double average(List<Double> values) {
            return values.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
        }
    }
}
