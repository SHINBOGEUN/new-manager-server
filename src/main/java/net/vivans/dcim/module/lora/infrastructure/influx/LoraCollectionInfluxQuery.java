package net.vivans.dcim.module.lora.infrastructure.influx;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.vivans.dcim.module.query.config.InfluxProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * LoRa 수집 검증(Ops Console "LoRa 수집 상태" 탭) 전용 InfluxDB 조회.
 * 기존 SNMP/PUE 쿼리 경로(module.query.*)는 전혀 건드리지 않고, 같은 InfluxDBClient/InfluxProperties 빈만 재사용한다.
 * device 목록 전체를 한 번에 조회하는 배치 쿼리이며, device별 N+1 조회를 하지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoraCollectionInfluxQuery {

    private static final int LOOKBACK_HOURS = 168;

    private final ObjectProvider<InfluxDBClient> clientProvider;
    private final InfluxProperties properties;

    public boolean isAvailable() {
        return properties.isEnabled() && clientProvider.getIfAvailable() != null;
    }

    /**
     * deviceId별로 protocol="mqtt" 포인트의 point_name별 마지막 값/시각을 배치 조회한다.
     * deviceName 태그 존재를 가정하지 않고 device_id/point_name/protocol 태그만 사용한다.
     */
    public Map<Integer, List<LoraInfluxPointRecord>> findRecentMqttPoints(List<Integer> deviceIds) {
        InfluxDBClient client = clientProvider.getIfAvailable();
        if (deviceIds == null || deviceIds.isEmpty() || client == null || !properties.isEnabled()) {
            return Map.of();
        }
        String flux = buildQuery(properties.getBucket(), properties.getMeasurement(), deviceIds);
        try {
            List<FluxTable> tables = client.getQueryApi().query(flux, properties.getOrg());
            return mapByDevice(tables);
        } catch (RuntimeException exception) {
            log.error("[LORA_COLLECTION_STATUS_INFLUX_QUERY_FAILED] exception={} message={}",
                    exception.getClass().getSimpleName(), exception.getMessage());
            return Map.of();
        }
    }

    private static String buildQuery(String bucket, String measurement, List<Integer> deviceIds) {
        String deviceFilter = deviceIds.stream()
                .map(id -> "r[\"device_id\"] == " + quote(String.valueOf(id)))
                .collect(Collectors.joining(" or "));
        return """
                from(bucket: %s)
                  |> range(start: -%dh)
                  |> filter(fn: (r) => r["_measurement"] == %s)
                  |> filter(fn: (r) => r["_field"] == "value")
                  |> filter(fn: (r) => r["protocol"] == "mqtt")
                  |> filter(fn: (r) => %s)
                  |> group(columns: ["device_id", "point_name"])
                  |> last()
                  |> keep(columns: ["device_id", "point_name", "_value", "_time"])
                """.formatted(quote(bucket), LOOKBACK_HOURS, quote(measurement), deviceFilter);
    }

    private static Map<Integer, List<LoraInfluxPointRecord>> mapByDevice(List<FluxTable> tables) {
        Map<Integer, List<LoraInfluxPointRecord>> result = new HashMap<>();
        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                Integer deviceId = parseDeviceId(record.getValueByKey("device_id"));
                String pointName = asText(record.getValueByKey("point_name"));
                Double value = toDouble(record.getValue());
                Instant time = record.getTime();
                if (deviceId == null || pointName == null || time == null) {
                    continue;
                }
                result.computeIfAbsent(deviceId, key -> new ArrayList<>())
                        .add(new LoraInfluxPointRecord(pointName, value, time));
            }
        }
        return result;
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

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
