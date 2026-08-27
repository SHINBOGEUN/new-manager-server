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
import net.vivans.dcim.shared.exception.QueryException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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
            QueryApi queryApi = client.getQueryApi();
            List<FluxTable> tables = queryApi.query(flux, properties.getOrg());
            List<LastPoint> points = new ArrayList<>();
            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    LastPoint point = toPoint(record);
                    if (point != null) {
                        points.add(point);
                    }
                }
            }
            return points;
        } catch (RuntimeException exception) {
            log.error("Query last failed: {}", exception.getMessage(), exception);
            throw new QueryException("InfluxDB query failed");
        }
    }

    private static LastPoint toPoint(FluxRecord record) {
        Integer deviceId = parseDeviceId(record.getValueByKey("device_id"));
        String pointName = asText(record.getValueByKey("point_name"));
        Double value = toDouble(record.getValue());
        Instant time = record.getTime();
        if (deviceId == null || pointName == null || value == null || time == null) {
            return null;
        }
        return new LastPoint(deviceId, pointName, value, time);
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
