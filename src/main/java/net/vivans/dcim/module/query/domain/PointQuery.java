package net.vivans.dcim.module.query.domain;

import java.time.Duration;
import java.util.List;

public interface PointQuery {

    List<LastPoint> findLast(List<Integer> deviceIds, List<String> pointNames, Duration lookback);
}
