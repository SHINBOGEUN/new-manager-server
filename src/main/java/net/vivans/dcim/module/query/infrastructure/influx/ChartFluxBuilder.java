package net.vivans.dcim.module.query.infrastructure.influx;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

final class ChartFluxBuilder {

    private ChartFluxBuilder() {
    }

    static String buildSeriesQuery(
            String bucket,
            String measurement,
            List<Integer> deviceIds,
            List<String> pointNames,
            Instant start,
            Instant end,
            String window
    ) {
        return """
                from(bucket: %s)
                  |> range(start: time(v: %s), stop: time(v: %s))
                  |> filter(fn: (r) => r["_measurement"] == %s)
                  |> filter(fn: (r) => r["_field"] == "value")
                  |> filter(fn: (r) => %s)
                  |> filter(fn: (r) => %s)
                  |> aggregateWindow(every: %s, fn: mean, createEmpty: false)
                  |> group(columns: ["device_id", "point_name"])
                  |> keep(columns: ["device_id", "point_name", "_value", "_time"])
                """.formatted(
                LastFluxBuilder.quote(bucket),
                LastFluxBuilder.quote(start.toString()),
                LastFluxBuilder.quote(end.toString()),
                LastFluxBuilder.quote(measurement),
                orEquals("device_id", deviceIds.stream().map(String::valueOf).toList()),
                orEquals("point_name", pointNames),
                window
        );
    }

    private static String orEquals(String tag, List<String> values) {
        return values.stream()
                .map(value -> "r[%s] == %s".formatted(LastFluxBuilder.quote(tag), LastFluxBuilder.quote(value)))
                .collect(Collectors.joining(" or "));
    }
}
