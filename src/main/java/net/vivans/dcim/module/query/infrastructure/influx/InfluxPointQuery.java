package net.vivans.dcim.module.query.infrastructure.influx;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.vivans.dcim.module.query.config.InfluxProperties;
import net.vivans.dcim.module.query.domain.LastPoint;
import net.vivans.dcim.module.query.domain.PointQuery;
import net.vivans.dcim.module.query.domain.PueLastPoint;
import net.vivans.dcim.module.query.domain.SeriesPoint;
import net.vivans.dcim.shared.exception.QueryException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class InfluxPointQuery implements PointQuery {

    private final InfluxDBClient client;
    private final InfluxProperties properties;

    @Override
    public List<LastPoint> findLast(List<Integer> deviceIds, List<String> pointNames, Duration lookback) {
        String flux = LastFluxBuilder.buildLastQuery(
                properties.getBucket(),
                properties.getMeasurement(),
                deviceIds,
                pointNames,
                lookback
        );
        try {
            return mapLast(query(flux));
        } catch (RuntimeException exception) {
            log.error("Query last failed: {}", exception.getMessage(), exception);
            throw new QueryException("InfluxDB query failed");
        }
    }

    @Override
    public List<SeriesPoint> findSeries(
            List<Integer> deviceIds,
            List<String> pointNames,
            Instant start,
            Instant end,
            String window
    ) {
        String flux = ChartFluxBuilder.buildSeriesQuery(
                properties.getBucket(),
                properties.getMeasurement(),
                deviceIds,
                pointNames,
                start,
                end,
                window
        );
        try {
            return mapSeries(query(flux));
        } catch (RuntimeException exception) {
            log.error("Query series failed: {}", exception.getMessage(), exception);
            throw new QueryException("InfluxDB query failed");
        }
    }

    @Override
    public List<LastPoint> findFirstInRange(
            List<Integer> deviceIds,
            List<String> pointNames,
            Instant start,
            Instant end
    ) {
        String flux = BoundaryFluxBuilder.buildFirstQuery(
                properties.getBucket(),
                properties.getMeasurement(),
                deviceIds,
                pointNames,
                start,
                end
        );
        try {
            return mapLast(query(flux));
        } catch (RuntimeException exception) {
            log.error("Query first-in-range failed: {}", exception.getMessage(), exception);
            throw new QueryException("InfluxDB query failed");
        }
    }

    @Override
    public List<LastPoint> findLastInRange(
            List<Integer> deviceIds,
            List<String> pointNames,
            Instant start,
            Instant end
    ) {
        String flux = BoundaryFluxBuilder.buildLastQuery(
                properties.getBucket(),
                properties.getMeasurement(),
                deviceIds,
                pointNames,
                start,
                end
        );
        try {
            return mapLast(query(flux));
        } catch (RuntimeException exception) {
            log.error("Query last-in-range failed: {}", exception.getMessage(), exception);
            throw new QueryException("InfluxDB query failed");
        }
    }

    @Override
    public Optional<PueLastPoint> findLastPue(Integer definitionId, Duration lookback) {
        String flux = LastFluxBuilder.buildPueLastQuery(
                properties.getBucket(),
                properties.getMeasurement(),
                definitionId,
                lookback
        );
        try {
            return mapPueLast(query(flux));
        } catch (RuntimeException exception) {
            log.error("Query last PUE failed definitionId={}: {}", definitionId, exception.getMessage(), exception);
            throw new QueryException("InfluxDB query failed");
        }
    }

    private List<FluxTable> query(String flux) {
        QueryApi queryApi = client.getQueryApi();
        return queryApi.query(flux, properties.getOrg());
    }

    private static List<LastPoint> mapLast(List<FluxTable> tables) {
        List<LastPoint> points = new ArrayList<>();
        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                LastPoint point = toLastPoint(record);
                if (point != null) {
                    points.add(point);
                }
            }
        }
        return points;
    }

    private static List<SeriesPoint> mapSeries(List<FluxTable> tables) {
        List<SeriesPoint> points = new ArrayList<>();
        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                SeriesPoint point = toSeriesPoint(record);
                if (point != null) {
                    points.add(point);
                }
            }
        }
        return points;
    }

    private static Optional<PueLastPoint> mapPueLast(List<FluxTable> tables) {
        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                Double value = toDouble(record.getValueByKey("value"));
                Instant time = record.getTime();
                if (value != null && time != null) {
                    return Optional.of(new PueLastPoint(
                            value,
                            toDouble(record.getValueByKey("total_power")),
                            toDouble(record.getValueByKey("cooler_power")),
                            time
                    ));
                }
            }
        }
        return Optional.empty();
    }

    private static LastPoint toLastPoint(FluxRecord record) {
        Integer deviceId = parseDeviceId(record.getValueByKey("device_id"));
        String pointName = asText(record.getValueByKey("point_name"));
        Double value = toDouble(record.getValue());
        Instant time = record.getTime();
        if (deviceId == null || pointName == null || value == null || time == null) {
            return null;
        }
        return new LastPoint(deviceId, pointName, value, time);
    }

    private static SeriesPoint toSeriesPoint(FluxRecord record) {
        Integer deviceId = parseDeviceId(record.getValueByKey("device_id"));
        String pointName = asText(record.getValueByKey("point_name"));
        Double value = toDouble(record.getValue());
        Instant time = record.getTime();
        if (deviceId == null || pointName == null || value == null || time == null) {
            return null;
        }
        return new SeriesPoint(deviceId, pointName, value, time);
    }

    private static Integer parseDeviceId(Object raw) {
        if (raw instanceof Number number) {
            return number.intValue();
        }
        String text = asText(raw);
        if (text == null) {
            return null;
        }
        try {
            return Integer.valueOf(text);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static Double toDouble(Object raw) {
        if (raw instanceof Number number) {
            double converted = number.doubleValue();
            return Double.isFinite(converted) ? converted : null;
        }
        return null;
    }

    private static String asText(Object raw) {
        if (raw == null) {
            return null;
        }
        String text = String.valueOf(raw).trim();
        return text.isEmpty() ? null : text;
    }
}
