package net.vivans.dcim.module.query.infrastructure.influx;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LastFluxBuilderTest {

    @Test
    void buildsNarrowLastQuery() {
        String flux = LastFluxBuilder.buildLastQuery(
                "dcim",
                "dcim_sensor",
                List.of(101, 102),
                List.of("V", "temp"),
                Duration.ofHours(24)
        );

        assertThat(flux).contains("from(bucket: \"dcim\")");
        assertThat(flux).contains("range(start: -24h)");
        assertThat(flux).contains("r[\"_measurement\"] == \"dcim_sensor\"");
        assertThat(flux).contains("r[\"_field\"] == \"value\"");
        assertThat(flux).contains("r[\"device_id\"] == \"101\" or r[\"device_id\"] == \"102\"");
        assertThat(flux).contains("r[\"point_name\"] == \"V\" or r[\"point_name\"] == \"temp\"");
        assertThat(flux).contains("group(columns: [\"device_id\", \"point_name\"])");
        assertThat(flux).contains("last()");
        assertThat(flux).doesNotContain("TOTAL_WT");
        assertThat(flux).doesNotContain("pdu_deviceId");
    }

    @Test
    void escapesQuotesInTagValues() {
        String quoted = LastFluxBuilder.quote("a\"b");
        assertThat(quoted).isEqualTo("\"a\\\"b\"");
    }
}
