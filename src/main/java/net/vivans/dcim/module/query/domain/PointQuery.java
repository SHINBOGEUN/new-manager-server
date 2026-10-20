package net.vivans.dcim.module.query.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PointQuery {

    List<LastPoint> findLast(List<Integer> deviceIds, List<String> pointNames, Duration lookback);

    default List<LastPoint> findLast(List<Integer> deviceIds, List<String> pointNames, Duration lookback,
                                     String protocol) {
        return findLast(deviceIds, pointNames, lookback);
    }

    List<SeriesPoint> findSeries(
            List<Integer> deviceIds,
            List<String> pointNames,
            Instant start,
            Instant end,
            String window
    );

    List<SeriesPoint> findRawSeries(
            List<Integer> deviceIds,
            List<String> pointNames,
            Instant start,
            Instant end
    );

    default List<SeriesPoint> findRawSeries(List<Integer> deviceIds, List<String> pointNames,
                                            Instant start, Instant end, String protocol) {
        return findRawSeries(deviceIds, pointNames, start, end);
    }

    List<SeriesPoint> findLatestRaw(List<Integer> deviceIds, List<String> pointNames,
                                    Instant end, String protocol);

    List<StatsSeriesPoint> findStatsSeries(
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

    Optional<CalculatedMetricLastPoint> findLastCalculated(Integer definitionId, Integer configVersion, Duration lookback);

    Optional<CalculatedMetricLastPoint> findLastCalculatedPreviousVersion(Integer definitionId, Integer currentConfigVersion,
                                                                        Duration lookback);

    List<CalculatedMetricSeriesPoint> findCalculatedSeries(Integer definitionId, Instant start, Instant end, String window);
}
