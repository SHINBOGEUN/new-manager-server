package net.vivans.dcim.module.query.application;

import net.vivans.dcim.module.device.api.dto.DevicePageOptionsResponse;
import net.vivans.dcim.module.device.api.dto.DevicePageOptionsResponse.*;
import net.vivans.dcim.module.device.application.DevicePageOptionsQueryService;
import net.vivans.dcim.module.query.api.dto.AnalysisMeasurementsRequest;
import net.vivans.dcim.module.query.domain.*;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.*;

class AnalysisGpuQueryServiceTest {
    final DevicePageOptionsQueryService options = mock(DevicePageOptionsQueryService.class);
    final PointQuery query = mock(PointQuery.class);
    final AnalysisQueryService service = new AnalysisQueryService(options, query);
    final Instant time = Instant.parse("2026-10-15T00:00:00Z");
    void setup() {
        var power = new PointOption(1, "mqtt", 1, 12, "power", "W", "POWER", null, "POWER", null, "POWER", "전력", null, true, "MODEL_POINT", 0, "COMPONENT", "GPU");
        var memory = new PointOption(1, "mqtt", 2, 12, "memory_used_GB", "GiB", "MEMORY", null, null, null, null, "메모리", null, true, "MODEL_POINT", 1);
        when(options.getOptions("ANALYSIS")).thenReturn(new DevicePageOptionsResponse("ANALYSIS", "분석", List.of(
                new DeviceOption(33, "GPU 서버 1", "GPU_SERVER", 12, "GPU", null, null, true, List.of(power, memory)))));
    }
    AnalysisMeasurementsRequest request(List<String> names, List<String> indices, boolean stats) {
        return new AnalysisMeasurementsRequest(LocalDate.parse(stats ? "2026-10-01" : "2026-10-15"), LocalDate.parse("2026-10-15"),
                List.of(new AnalysisMeasurementsRequest.Target(33, names, indices)));
    }
    @Test void keepsIndicesSeparateAndDeviceValuesSeparate() {
        setup();
        when(query.findRawSeries(anyList(), anyList(), any(), any())).thenReturn(List.of(
                new SeriesPoint(33, "power", 250D, time, "GPU", "0"),
                new SeriesPoint(33, "power", 260D, time, "GPU", "1"),
                new SeriesPoint(33, "power", 999D, time, null, null),
                new SeriesPoint(33, "memory_used_GB", 1.2D, time)));
        var device = service.getMeasurements(request(List.of("power", "memory_used_GB"), null, false)).get(0);
        var fields = device.sensors().stream().flatMap(s -> s.units().stream()).flatMap(u -> u.fields().stream()).toList();
        assertThat(fields).hasSize(3);
        assertThat(fields.stream().filter(f -> f.pointName().equals("power")).map(f -> f.componentIndex()).toList()).containsExactly("0", "1");
        assertThat(fields.get(0).raws().get(0).value()).isEqualTo(250D);
    }
    @Test void filtersSelectedIndexAndKeepsSwapSamplesInOneSeries() {
        setup();
        when(query.findRawSeries(anyList(), anyList(), any(), any())).thenReturn(List.of(
                new SeriesPoint(33, "power", 250D, time, "GPU", "0"),
                new SeriesPoint(33, "power", 280D, time.plusSeconds(60), "GPU", "0"),
                new SeriesPoint(33, "power", 260D, time, "GPU", "1")));
        var fields = service.getMeasurements(request(List.of("power"), List.of("0"), false)).get(0).sensors().get(0).units().get(0).fields();
        assertThat(fields).hasSize(1);
        assertThat(fields.get(0).raws()).hasSize(2);
        assertThat(fields.get(0).displayName()).isEqualTo("GPU 0 · power");
    }
    @Test void usesCommonAggregationForComponents() {
        setup();
        when(query.findStatsSeries(anyList(), anyList(), any(), any(), eq("30m"))).thenReturn(List.of(
                new StatsSeriesPoint(33, "power", 200D, 280D, 250D, time, "GPU", "0")));
        var device = service.getMeasurements(request(List.of("power"), null, true)).get(0);
        assertThat(device.aggregate()).isEqualTo("30m");
        assertThat(device.sensors().get(0).units().get(0).fields().get(0).stats().get(0).avg()).isEqualTo(250D);
    }
    @Test void treeSelectionCombinesDeviceValueWithOnlyOneGpuPoint() {
        setup();
        when(query.findRawSeries(anyList(), anyList(), any(), any())).thenReturn(List.of(
                new SeriesPoint(33, "power", 250D, time, "GPU", "0"),
                new SeriesPoint(33, "power", 260D, time, "GPU", "1"),
                new SeriesPoint(33, "memory_used_GB", 1.2D, time)));
        var request = new AnalysisMeasurementsRequest(LocalDate.parse("2026-10-15"), LocalDate.parse("2026-10-15"),
                List.of(new AnalysisMeasurementsRequest.Target(33, List.of("power", "memory_used_GB"), null,
                        List.of(new AnalysisMeasurementsRequest.ComponentSelection("power", "GPU", "1")))));
        var fields = service.getMeasurements(request).get(0).sensors().stream().flatMap(s -> s.units().stream()).flatMap(u -> u.fields().stream()).toList();
        assertThat(fields).hasSize(2);
        assertThat(fields.stream().filter(f -> f.componentIndex() != null).map(f -> f.componentIndex()).toList()).containsExactly("1");
    }
    @Test void rejectsHiddenPointsBeforeInflux() {
        setup();
        assertThatThrownBy(() -> service.getComponents(request(List.of("hidden"), null, false))).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(query);
    }
    @Test void discoversOnlyAllowedDeviceAndComponentIndices() {
        setup();
        when(query.findStatsSeries(anyList(), anyList(), any(), any(), eq("1d"))).thenReturn(List.of(
                new StatsSeriesPoint(33, "power", 1D, 2D, 1D, time, "GPU", "4"),
                new StatsSeriesPoint(33, "power", 1D, 2D, 1D, time, "GPU", "0"),
                new StatsSeriesPoint(99, "power", 1D, 2D, 1D, time, "GPU", "7")));
        assertThat(service.getComponents(request(List.of("power"), null, false)).stream().map(c -> c.componentIndex()).toList()).containsExactly("0", "4");
    }
}
