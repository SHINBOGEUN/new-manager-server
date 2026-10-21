package net.vivans.dcim.module.query.infrastructure.influx;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

final class AnalysisFluxBuilder {

    private AnalysisFluxBuilder() {
    }

    static String buildRawQuery(
            String bucket,
            String measurement,
            List<Integer> deviceIds,
            List<String> pointNames,
            Instant start,
            Instant end
    ) {
        return buildRawQuery(bucket, measurement, deviceIds, pointNames, start, end, null);
    }

    static String buildRawQuery(String bucket, String measurement, List<Integer> deviceIds,
                                List<String> pointNames, Instant start, Instant end, String protocol) {
        return """
                from(bucket: %s)
                  |> range(start: time(v: %s), stop: time(v: %s))
                  |> filter(fn: (r) => r["_measurement"] == %s)
                  |> filter(fn: (r) => r["_field"] == "value")
                  |> filter(fn: (r) => %s)
                  |> filter(fn: (r) => %s)
                  %s
                  |> map(fn: (r) => ({r with component_type: if exists r.component_type then r.component_type else "", component_index: if exists r.component_index then r.component_index else ""}))
                  |> group(columns: ["device_id", "point_name", "component_type", "component_index"])
                  |> keep(columns: ["device_id", "point_name", "component_type", "component_index", "_value", "_time"])
                """.formatted(
                LastFluxBuilder.quote(bucket),
                LastFluxBuilder.quote(start.toString()),
                LastFluxBuilder.quote(end.toString()),
                LastFluxBuilder.quote(measurement),
                orEquals("device_id", deviceIds.stream().map(String::valueOf).toList()),
                orEquals("point_name", pointNames),
                protocol == null ? "" : "|> filter(fn: (r) => r[\"protocol\"] == " + LastFluxBuilder.quote(protocol) + ")"
        );
    }

    static String buildLatestRawQuery(String bucket, String measurement, List<Integer> deviceIds,
                                      List<String> pointNames, Instant end, String protocol) {
        return """
                from(bucket: %s)
                  |> range(start: 0, stop: time(v: %s))
                  |> filter(fn: (r) => r["_measurement"] == %s)
                  |> filter(fn: (r) => r["_field"] == "value")
                  |> filter(fn: (r) => %s)
                  |> filter(fn: (r) => %s)
                  %s
                  |> map(fn: (r) => ({r with component_type: if exists r.component_type then r.component_type else ""}))
                  |> filter(fn: (r) => r.component_type == "")
                  |> group(columns: ["device_id", "point_name"])
                  |> last()
                  |> keep(columns: ["device_id", "point_name", "component_type", "component_index", "_value", "_time"])
                """.formatted(
                LastFluxBuilder.quote(bucket),
                LastFluxBuilder.quote(end.toString()),
                LastFluxBuilder.quote(measurement),
                orEquals("device_id", deviceIds.stream().map(String::valueOf).toList()),
                orEquals("point_name", pointNames),
                protocol == null ? "" : "|> filter(fn: (r) => r[\"protocol\"] == " + LastFluxBuilder.quote(protocol) + ")"
        );
    }

    static String buildStatsQuery(
            String bucket,
            String measurement,
            List<Integer> deviceIds,
            List<String> pointNames,
            Instant start,
            Instant end,
            String window
    ) {
        String filtered = """
                from(bucket: %s)
                  |> range(start: time(v: %s), stop: time(v: %s))
                  |> filter(fn: (r) => r["_measurement"] == %s)
                  |> filter(fn: (r) => r["_field"] == "value")
                  |> filter(fn: (r) => %s)
                  |> filter(fn: (r) => %s)
                  |> map(fn: (r) => ({r with component_type: if exists r.component_type then r.component_type else "", component_index: if exists r.component_index then r.component_index else ""}))
                  |> group(columns: ["device_id", "point_name", "component_type", "component_index"])
                """.formatted(
                LastFluxBuilder.quote(bucket),
                LastFluxBuilder.quote(start.toString()),
                LastFluxBuilder.quote(end.toString()),
                LastFluxBuilder.quote(measurement),
                orEquals("device_id", deviceIds.stream().map(String::valueOf).toList()),
                orEquals("point_name", pointNames)
        );
        return """
                data = %s
                mins = data |> aggregateWindow(every: %s, fn: min, createEmpty: false)
                  |> map(fn: (r) => ({r with statistic: "min"}))
                maxs = data |> aggregateWindow(every: %s, fn: max, createEmpty: false)
                  |> map(fn: (r) => ({r with statistic: "max"}))
                means = data |> aggregateWindow(every: %s, fn: mean, createEmpty: false)
                  |> map(fn: (r) => ({r with statistic: "mean"}))
                union(tables: [mins, maxs, means])
                  |> pivot(rowKey: ["_time", "device_id", "point_name", "component_type", "component_index"], columnKey: ["statistic"], valueColumn: "_value")
                  |> keep(columns: ["device_id", "point_name", "component_type", "component_index", "_time", "min", "max", "mean"])
                """.formatted(filtered, window, window, window);
    }

    private static String orEquals(String tag, List<String> values) {
        return values.stream()
                .map(value -> "r[%s] == %s".formatted(
                        LastFluxBuilder.quote(tag), LastFluxBuilder.quote(value)))
                .collect(Collectors.joining(" or "));
    }
}
