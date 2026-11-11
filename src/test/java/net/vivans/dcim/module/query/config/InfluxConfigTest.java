package net.vivans.dcim.module.query.config;

import io.micrometer.core.instrument.Clock;
import io.micrometer.influx.InfluxMeterRegistry;
import net.vivans.dcim.module.query.domain.PointQuery;
import net.vivans.dcim.module.query.infrastructure.influx.DisabledPointQuery;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.autoconfigure.metrics.export.influx.InfluxMetricsExportAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class InfluxConfigTest {
    @Test
    void queryConfigurationCanCoexistWithInfluxMetricsAutoConfiguration() {
        new ApplicationContextRunner()
                .withUserConfiguration(InfluxConfig.class)
                .withConfiguration(AutoConfigurations.of(InfluxMetricsExportAutoConfiguration.class))
                .withBean(Clock.class, () -> Clock.SYSTEM)
                .withBean(InfluxMeterRegistry.class, () -> mock(InfluxMeterRegistry.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("dcimInfluxQueryConfiguration");
                    assertThat(context).hasBean("influxConfig");
                    assertThat(context).getBean(PointQuery.class).isInstanceOf(DisabledPointQuery.class);
                });
    }
}
