package net.vivans.dcim.module.query.infrastructure.influx;

import lombok.extern.slf4j.Slf4j;
import net.vivans.dcim.module.query.domain.LastPoint;
import net.vivans.dcim.module.query.domain.PointQuery;
import net.vivans.dcim.module.query.domain.CalculatedMetricLastPoint;
import net.vivans.dcim.module.query.domain.CalculatedMetricSeriesPoint;
import net.vivans.dcim.module.query.domain.SeriesPoint;
import net.vivans.dcim.module.query.domain.StatsSeriesPoint;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
public class DisabledPointQuery implements PointQuery {

    @Override
    public List<LastPoint> findLast(List<Integer> deviceIds, List<String> pointNames, Duration lookback) {
        log.warn("InfluxDB query disabled; returning empty last values");
        return List.of();
    }

    @Override
    public List<SeriesPoint> findSeries(
            List<Integer> deviceIds,
            List<String> pointNames,
            Instant start,
            Instant end,
            String window
    ) {
        log.warn("InfluxDB query disabled; returning empty series values");
        return List.of();
    }

    @Override
    public List<SeriesPoint> findRawSeries(
            List<Integer> deviceIds,
            List<String> pointNames,
            Instant start,
            Instant end
    ) {
        log.warn("InfluxDB query disabled; returning empty raw analysis values");
        return List.of();
    }

    @Override
    public List<SeriesPoint> findLatestRaw(List<Integer> deviceIds, List<String> pointNames,
                                           Instant end, String protocol) {
        log.warn("InfluxDB query disabled; returning empty latest raw values");
        return List.of();
    }

    @Override
    public List<StatsSeriesPoint> findStatsSeries(
            List<Integer> deviceIds,
            List<String> pointNames,
            Instant start,
            Instant end,
            String window
    ) {
        log.warn("InfluxDB query disabled; returning empty aggregated analysis values");
        return List.of();
    }

    @Override
    public List<LastPoint> findFirstInRange(
            List<Integer> deviceIds,
            List<String> pointNames,
            Instant start,
            Instant end
    ) {
        log.warn("InfluxDB query disabled; returning empty first-in-range values");
        return List.of();
    }

    @Override
    public List<LastPoint> findLastInRange(
            List<Integer> deviceIds,
            List<String> pointNames,
            Instant start,
            Instant end
    ) {
        log.warn("InfluxDB query disabled; returning empty last-in-range values");
        return List.of();
    }

    @Override
    public Optional<CalculatedMetricLastPoint> findLastCalculated(Integer definitionId, Integer configVersion, Duration lookback) {
        log.warn("InfluxDB query disabled; returning empty calculated value definitionId={}", definitionId);
        return Optional.empty();
    }

    @Override
    public Optional<CalculatedMetricLastPoint> findLastCalculatedPreviousVersion(Integer definitionId,
                                                                                 Integer currentConfigVersion,
                                                                                 Duration lookback) {
        log.warn("InfluxDB query disabled; returning empty previous calculated value definitionId={}", definitionId);
        return Optional.empty();
    }

    @Override
    public List<CalculatedMetricSeriesPoint> findCalculatedSeries(Integer definitionId, Instant start, Instant end, String window) {
        log.warn("InfluxDB query disabled; returning empty calculated metric series definitionId={}", definitionId);
        return List.of();
    }
}
