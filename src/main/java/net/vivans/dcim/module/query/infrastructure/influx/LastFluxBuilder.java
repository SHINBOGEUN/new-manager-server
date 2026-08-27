package net.vivans.dcim.module.query.infrastructure.influx;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

final class LastFluxBuilder {

    private LastFluxBuilder() {
    }

    static String buildLastQuery(
            String bucket,
            String measurement,
            List<Integer> deviceIds,
            List<String> pointNames,
            Duration lookback
    ) {
        return """
                from(bucket: %s)
                  |> range(start: -%s)
                  |> filter(fn: (r) => r["_measurement"] == %s)
                  |> filter(fn: (r) => r["_field"] == "value")
                  |> filter(fn: (r) => %s)
                  |> filter(fn: (r) => %s)
                  |> group(columns: ["device_id", "point_name"])
                  |> last()
                  |> keep(columns: ["device_id", "point_name", "_value", "_time"])
                """.formatted(
                quote(bucket),
                toFluxDuration(lookback),
                quote(measurement),
                orEquals("device_id", deviceIds.stream().map(String::valueOf).toList()),
                orEquals("point_name", pointNames)
        );
    }

    private static String orEquals(String tag, List<String> values) {
        return values.stream()
                .map(value -> "r[%s] == %s".formatted(quote(tag), quote(value)))
                .collect(Collectors.joining(" or "));
    }

    static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    static String toFluxDuration(Duration lookback) {
        long hours = Math.max(1, lookback.toHours());
        return hours + "h";
    }
}
