package net.vivans.dcim.module.query.infrastructure.influx;

import lombok.extern.slf4j.Slf4j;
import net.vivans.dcim.module.query.domain.LastPoint;
import net.vivans.dcim.module.query.domain.PointQuery;
import net.vivans.dcim.module.query.domain.SeriesPoint;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

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
}
