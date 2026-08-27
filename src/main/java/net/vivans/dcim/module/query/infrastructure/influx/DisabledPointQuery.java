package net.vivans.dcim.module.query.infrastructure.influx;

import lombok.extern.slf4j.Slf4j;
import net.vivans.dcim.module.query.domain.LastPoint;
import net.vivans.dcim.module.query.domain.PointQuery;

import java.time.Duration;
import java.util.List;

@Slf4j
public class DisabledPointQuery implements PointQuery {

    @Override
    public List<LastPoint> findLast(List<Integer> deviceIds, List<String> pointNames, Duration lookback) {
        log.warn("InfluxDB query disabled; returning empty last values");
        return List.of();
    }
}
