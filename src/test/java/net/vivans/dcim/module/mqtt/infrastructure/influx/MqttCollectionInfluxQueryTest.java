package net.vivans.dcim.module.mqtt.infrastructure.influx;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class MqttCollectionInfluxQueryTest {
    @Test void separatesGpuSlotsButMergesReplacedUuidsAndUsesTimeOrder() {
        String flux = MqttCollectionInfluxQuery.buildQuery("dcim", "dcim_sensor", List.of(33));
        assertThat(flux).contains("range(start: -168h)", "r[\"device_id\"] == \"33\"",
                "if exists r.component_index", "if exists r.component_type",
                "group(columns: [\"device_id\", \"point_name\", \"component_type\", \"component_index\"])",
                "sort(columns: [\"_time\"])", "last()");
        assertThat(flux).doesNotContain("component_uuid");
    }
}
