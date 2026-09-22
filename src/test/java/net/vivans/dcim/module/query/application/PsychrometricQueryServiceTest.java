package net.vivans.dcim.module.query.application;

import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.model.PageWidget;
import net.vivans.dcim.module.device.domain.model.PageWidgetPsychrometric;
import net.vivans.dcim.module.device.domain.model.PageWidgetPsychrometricSource;
import net.vivans.dcim.module.device.domain.model.PageWidgetPsychrometricSourceRole;
import net.vivans.dcim.module.device.domain.model.PageWidgetQueryKind;
import net.vivans.dcim.module.device.domain.repository.PageWidgetRepository;
import net.vivans.dcim.module.query.api.dto.PsychrometricWidgetResponse;
import net.vivans.dcim.module.query.domain.LastPoint;
import net.vivans.dcim.module.query.domain.PointQuery;
import net.vivans.dcim.module.query.domain.SeriesPoint;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PsychrometricQueryServiceTest {

    @Mock
    private PageWidgetRepository pageWidgetRepository;
    @Mock
    private PointQuery pointQuery;
    @org.mockito.Spy
    private WidgetDataStatusResolver widgetDataStatusResolver = new WidgetDataStatusResolver();
    @InjectMocks
    private PsychrometricQueryService service;

    @Test
    void returnsHourlyAveragesAndLatestAverageFromInflux() {
        PageWidget widget = org.mockito.Mockito.mock(PageWidget.class);
        PageWidgetPsychrometric psychrometric = org.mockito.Mockito.mock(PageWidgetPsychrometric.class);
        Instant historyTime = Instant.parse("2026-09-08T06:00:00Z");
        Instant latestTime = Instant.parse("2026-09-08T06:19:00Z");
        Set<PageWidgetPsychrometricSource> sources = new LinkedHashSet<>(List.of(
                source(1, "TEMP", PageWidgetPsychrometricSourceRole.temperature),
                source(2, "TEMP", PageWidgetPsychrometricSourceRole.temperature),
                source(1, "HUM", PageWidgetPsychrometricSourceRole.humidity),
                source(2, "HUM", PageWidgetPsychrometricSourceRole.humidity)
        ));

        when(pageWidgetRepository.findById(77)).thenReturn(Optional.of(widget));
        when(widget.getQueryKind()).thenReturn(PageWidgetQueryKind.psychrometric);
        when(widget.isEnabled()).thenReturn(true);
        when(widget.getName()).thenReturn("환경 습공기선");
        when(widget.getPsychrometric()).thenReturn(psychrometric);
        when(psychrometric.getSources()).thenReturn(sources);
        when(pointQuery.findSeries(anyList(), anyList(), any(), any(), any()))
                .thenReturn(List.of(
                        new SeriesPoint(1, "TEMP", 24.0, historyTime),
                        new SeriesPoint(2, "TEMP", 26.0, historyTime),
                        new SeriesPoint(1, "HUM", 40.0, historyTime),
                        new SeriesPoint(2, "HUM", 50.0, historyTime)
                ));
        when(pointQuery.findLast(anyList(), anyList(), any()))
                .thenReturn(List.of(
                        new LastPoint(1, "TEMP", 27.0, latestTime),
                        new LastPoint(2, "TEMP", 29.0, latestTime),
                        new LastPoint(1, "HUM", 41.0, latestTime),
                        new LastPoint(2, "HUM", 43.0, latestTime)
                ));

        PsychrometricWidgetResponse response = service.getPsychrometric(77);

        assertThat(response.title()).isEqualTo("환경 습공기선");
        assertThat(response.timeLabels()).containsExactly(historyTime, latestTime);
        assertThat(response.data()).hasSize(2);
        assertThat(response.data().get(0).label()).isEqualTo("TEMP_AVG");
        assertThat(response.data().get(0).values()).containsExactly(25.0, 28.0);
        assertThat(response.data().get(0).unit()).isEqualTo("°C");
        assertThat(response.data().get(1).label()).isEqualTo("HUM_AVG");
        assertThat(response.data().get(1).values()).containsExactly(45.0, 42.0);
        assertThat(response.data().get(1).unit()).isEqualTo("%");
    }

    private static PageWidgetPsychrometricSource source(
            int deviceId,
            String pointName,
            PageWidgetPsychrometricSourceRole role
    ) {
        Device device = org.mockito.Mockito.mock(Device.class);
        PageWidgetPsychrometricSource source = org.mockito.Mockito.mock(PageWidgetPsychrometricSource.class);
        when(device.getId()).thenReturn(deviceId);
        when(source.getDevice()).thenReturn(device);
        when(source.getPointName()).thenReturn(pointName);
        when(source.getRole()).thenReturn(role);
        return source;
    }
}
