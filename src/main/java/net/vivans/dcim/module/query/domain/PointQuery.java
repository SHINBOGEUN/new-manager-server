package net.vivans.dcim.module.query.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public interface PointQuery {

    List<LastPoint> findLast(List<Integer> deviceIds, List<String> pointNames, Duration lookback);

    List<SeriesPoint> findSeries(
            List<Integer> deviceIds,
            List<String> pointNames,
            Instant start,
            Instant end,
            String window
    );
}
