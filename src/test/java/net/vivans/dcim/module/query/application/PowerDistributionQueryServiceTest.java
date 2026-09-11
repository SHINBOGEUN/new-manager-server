package net.vivans.dcim.module.query.application;

import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.model.PageWidget;
import net.vivans.dcim.module.device.domain.model.PageWidgetPowerDistribution;
import net.vivans.dcim.module.device.domain.model.PageWidgetPowerDistributionGroup;
import net.vivans.dcim.module.device.domain.model.PageWidgetPowerDistributionSource;
import net.vivans.dcim.module.device.domain.model.PageWidgetQueryKind;
import net.vivans.dcim.module.device.domain.repository.PageWidgetRepository;
import net.vivans.dcim.module.query.api.dto.PowerDistributionWidgetResponse;
import net.vivans.dcim.module.query.domain.LastPoint;
import net.vivans.dcim.module.query.domain.PointQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PowerDistributionQueryServiceTest {

    @Mock
    private PageWidgetRepository pageWidgetRepository;
    @Mock
    private PointQuery pointQuery;
    @InjectMocks
    private PowerDistributionQueryService service;

    @Test
    void sumsWattsAndCalculatesRatiosWhenEveryConfiguredSourceHasAValue() {
        PageWidget widget = mock(PageWidget.class);
        PageWidgetPowerDistribution definition = mock(PageWidgetPowerDistribution.class);
        PageWidgetPowerDistributionGroup it = group("IT Power", "#38BDF8", source(1, "서버 #1", "TOTAL_W"));
        PageWidgetPowerDistributionGroup cooling = group("Cooling Power", "#A78BFA", source(2, "쿨러 #1", "TOTAL_W"));

        when(pageWidgetRepository.findById(77)).thenReturn(Optional.of(widget));
        when(widget.getId()).thenReturn(77);
        when(widget.getName()).thenReturn("전력 분배");
        when(widget.getQueryKind()).thenReturn(PageWidgetQueryKind.power_distribution);
        when(widget.isEnabled()).thenReturn(true);
        when(widget.getPowerDistribution()).thenReturn(definition);
        when(definition.getGroups()).thenReturn(new LinkedHashSet<>(List.of(it, cooling)));
        when(pointQuery.findLast(anyList(), anyList(), any()))
                .thenReturn(List.of(
                        new LastPoint(1, "TOTAL_W", 800.0, Instant.parse("2026-09-10T01:00:00Z")),
                        new LastPoint(2, "TOTAL_W", 200.0, Instant.parse("2026-09-10T01:00:00Z"))
                ));

        PowerDistributionWidgetResponse response = service.getPowerDistribution(77);

        assertThat(response.complete()).isTrue();
        assertThat(response.totalPowerW()).isEqualByComparingTo("1000.00");
        assertThat(response.groups()).extracting(group -> group.powerW().doubleValue()).containsExactly(800.0, 200.0);
        assertThat(response.groups()).extracting(group -> group.ratio().doubleValue()).containsExactly(80.0, 20.0);
    }

    @Test
    void returnsNoTotalOrRatioWhenAnyConfiguredSourceHasNoRecentInfluxValue() {
        PageWidget widget = mock(PageWidget.class);
        PageWidgetPowerDistribution definition = mock(PageWidgetPowerDistribution.class);
        PageWidgetPowerDistributionGroup it = group("IT Power", "#38BDF8", source(1, "서버 #1", "TOTAL_W"));
        PageWidgetPowerDistributionGroup cooling = group("Cooling Power", "#A78BFA", source(2, "쿨러 #1", "TOTAL_W"));

        when(pageWidgetRepository.findById(77)).thenReturn(Optional.of(widget));
        when(widget.getQueryKind()).thenReturn(PageWidgetQueryKind.power_distribution);
        when(widget.isEnabled()).thenReturn(true);
        when(widget.getPowerDistribution()).thenReturn(definition);
        when(definition.getGroups()).thenReturn(new LinkedHashSet<>(List.of(it, cooling)));
        when(pointQuery.findLast(anyList(), anyList(), any()))
                .thenReturn(List.of(new LastPoint(1, "TOTAL_W", 800.0, Instant.parse("2026-09-10T01:00:00Z"))));

        PowerDistributionWidgetResponse response = service.getPowerDistribution(77);

        assertThat(response.complete()).isFalse();
        assertThat(response.totalPowerW()).isNull();
        assertThat(response.groups().get(0).powerW()).isEqualByComparingTo("800.0");
        assertThat(response.groups().get(1).powerW()).isNull();
        assertThat(response.groups()).allSatisfy(group -> assertThat(group.ratio()).isNull());
    }

    private static PageWidgetPowerDistributionGroup group(
            String name, String color, PageWidgetPowerDistributionSource... sources
    ) {
        PageWidgetPowerDistributionGroup group = mock(PageWidgetPowerDistributionGroup.class);
        when(group.getName()).thenReturn(name);
        when(group.getColor()).thenReturn(color);
        when(group.getSources()).thenReturn(new LinkedHashSet<>(List.of(sources)));
        return group;
    }

    private static PageWidgetPowerDistributionSource source(int deviceId, String deviceName, String pointName) {
        Device device = mock(Device.class);
        PageWidgetPowerDistributionSource source = mock(PageWidgetPowerDistributionSource.class);
        when(device.getId()).thenReturn(deviceId);
        when(device.getName()).thenReturn(deviceName);
        when(source.getDevice()).thenReturn(device);
        when(source.getPointName()).thenReturn(pointName);
        return source;
    }
}
