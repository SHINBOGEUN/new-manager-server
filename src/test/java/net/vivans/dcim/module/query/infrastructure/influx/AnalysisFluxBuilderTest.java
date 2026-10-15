package net.vivans.dcim.module.query.infrastructure.influx;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnalysisFluxBuilderTest {

    private static final Instant START = Instant.parse("2026-09-30T15:00:00Z");
    private static final Instant END = Instant.parse("2026-10-07T15:00:00Z");

    @Test
    void buildsRawAnalysisQueryWithoutWindowAggregation() {
        String flux = AnalysisFluxBuilder.buildRawQuery(
                "dcim", "dcim_sensor", List.of(17), List.of("AMP"), START, END);

        assertThat(flux).contains("range(start: time(v: \"2026-09-30T15:00:00Z\"), stop: time(v: \"2026-10-07T15:00:00Z\"))");
        assertThat(flux).contains("r[\"device_id\"] == \"17\"");
        assertThat(flux).contains("r[\"point_name\"] == \"AMP\"");
        assertThat(flux).contains("keep(columns: [\"device_id\", \"point_name\", \"component_type\", \"component_index\", \"_value\", \"_time\"])");
        assertThat(flux).doesNotContain("aggregateWindow");
    }

    @Test
    void buildsMinMaxMeanQueryForRequestedWindow() {
        String flux = AnalysisFluxBuilder.buildStatsQuery(
                "dcim", "dcim_sensor", List.of(17), List.of("AMP"), START, END, "30m");

        assertThat(flux).contains("aggregateWindow(every: 30m, fn: min, createEmpty: false)");
        assertThat(flux).contains("aggregateWindow(every: 30m, fn: max, createEmpty: false)");
        assertThat(flux).contains("aggregateWindow(every: 30m, fn: mean, createEmpty: false)");
        assertThat(flux).contains("pivot(rowKey: [\"_time\", \"device_id\", \"point_name\", \"component_type\", \"component_index\"]");
        assertThat(flux).doesNotContain("component_uuid");
    }
}
