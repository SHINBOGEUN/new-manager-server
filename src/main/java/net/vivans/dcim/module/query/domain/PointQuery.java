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

    /** 구간 내 device+point별 첫 샘플 */
    List<LastPoint> findFirstInRange(
            List<Integer> deviceIds,
            List<String> pointNames,
            Instant start,
            Instant end
    );

    /** 구간 내 device+point별 마지막 샘플 */
    List<LastPoint> findLastInRange(
            List<Integer> deviceIds,
            List<String> pointNames,
            Instant start,
            Instant end
    );
}
