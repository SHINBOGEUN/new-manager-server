package net.vivans.dcim.module.query.infrastructure.influx;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

final class BoundaryFluxBuilder {

    private BoundaryFluxBuilder() {
    }

    static String buildFirstQuery(
            String bucket,
            String measurement,
            List<Integer> deviceIds,
            List<String> pointNames,
            Instant start,
            Instant end
    ) {
        return buildBoundaryQuery(bucket, measurement, deviceIds, pointNames, start, end, "first");
    }

    static String buildLastQuery(
            String bucket,
            String measurement,
            List<Integer> deviceIds,
            List<String> pointNames,
            Instant start,
            Instant end
    ) {
        return buildBoundaryQuery(bucket, measurement, deviceIds, pointNames, start, end, "last");
    }

    private static String buildBoundaryQuery(
            String bucket,
            String measurement,
            List<Integer> deviceIds,
            List<String> pointNames,
            Instant start,
            Instant end,
            String selector
    ) {
        return """
                from(bucket: %s)
                  |> range(start: time(v: %s), stop: time(v: %s))
                  |> filter(fn: (r) => r["_measurement"] == %s)
                  |> filter(fn: (r) => r["_field"] == "value")
                  |> filter(fn: (r) => %s)
                  |> filter(fn: (r) => %s)
                  |> group(columns: ["device_id", "point_name"])
                  |> %s()
                  |> keep(columns: ["device_id", "point_name", "_value", "_time"])
                """.formatted(
                LastFluxBuilder.quote(bucket),
                LastFluxBuilder.quote(start.toString()),
                LastFluxBuilder.quote(end.toString()),
                LastFluxBuilder.quote(measurement),
                orEquals("device_id", deviceIds.stream().map(String::valueOf).toList()),
                orEquals("point_name", pointNames),
                selector
        );
    }

    private static String orEquals(String tag, List<String> values) {
        return values.stream()
                .map(value -> "r[%s] == %s".formatted(LastFluxBuilder.quote(tag), LastFluxBuilder.quote(value)))
                .collect(Collectors.joining(" or "));
    }
}
