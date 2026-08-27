package net.vivans.dcim.module.query.application;

import net.vivans.dcim.module.common.domain.model.CodeGroup;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.model.DevicePageCodes;
import net.vivans.dcim.module.device.domain.model.PageWidget;
import net.vivans.dcim.module.device.domain.model.PageWidgetChartRangePreset;
import net.vivans.dcim.module.device.domain.model.PageWidgetChartScope;
import net.vivans.dcim.module.device.domain.model.PageWidgetChartSeriesMode;
import net.vivans.dcim.module.device.domain.model.PageWidgetDevice;
import net.vivans.dcim.module.device.domain.model.PageWidgetQueryKind;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import net.vivans.dcim.module.device.domain.repository.PageWidgetRepository;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModel;
import net.vivans.dcim.module.devicemodel.domain.repository.DeviceModelSnmpPointRepository;
import net.vivans.dcim.module.location.domain.model.LocationNode;
import net.vivans.dcim.module.query.api.dto.ChartWidgetResponse;
import net.vivans.dcim.module.query.domain.PointQuery;
import net.vivans.dcim.module.query.domain.SeriesPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChartQueryServiceTest {

    @Mock
    private PageWidgetRepository pageWidgetRepository;
    @Mock
    private DeviceRepository deviceRepository;
    @Mock
    private PointQuery pointQuery;
    @Mock
    private DeviceModelSnmpPointRepository deviceModelSnmpPointRepository;

    @InjectMocks
    private ChartQueryService service;

    @Test
    void perDevice_buildsSeriesPerDeviceAndPoint() {
        DeviceModel model = model(10);
        Device d1 = device(1, "PDU-A", "R1", "랙1", model);
        PageWidget widget = chartWidget(
                PageWidgetChartScope.devices,
                PageWidgetChartSeriesMode.per_device,
                List.of("W"),
                List.of(d1),
                List.of()
        );
        when(pageWidgetRepository.findById(12)).thenReturn(Optional.of(widget));
        when(deviceModelSnmpPointRepository.findAllEnabledByDeviceModelIds(any())).thenReturn(List.of());
        Instant t1 = Instant.parse("2026-08-27T01:00:00Z");
        Instant t2 = Instant.parse("2026-08-27T01:05:00Z");
        when(pointQuery.findSeries(anyList(), anyList(), any(), any(), anyString()))
                .thenReturn(List.of(
                        new SeriesPoint(1, "W", 10.0, t1),
                        new SeriesPoint(1, "W", 12.0, t2)
                ));

        ChartWidgetResponse response = service.getChart(12, null, null, null);

        assertThat(response.seriesMode()).isEqualTo("per_device");
        assertThat(response.series()).hasSize(1);
        assertThat(response.series().get(0).label()).isEqualTo("PDU-A");
        assertThat(response.series().get(0).times()).hasSize(2);
        assertThat(response.series().get(0).values()).containsExactly(10.0, 12.0);
    }

    @Test
    void seriesModeOverride_usesSum() {
        DeviceModel model = model(10);
        Device d1 = device(1, "A", "R1", "랙1", model);
        PageWidget widget = chartWidget(
                PageWidgetChartScope.devices,
                PageWidgetChartSeriesMode.by_phase,
                List.of("W"),
                List.of(d1),
                List.of()
        );
        when(pageWidgetRepository.findById(12)).thenReturn(Optional.of(widget));
        when(deviceModelSnmpPointRepository.findAllEnabledByDeviceModelIds(any())).thenReturn(List.of());
        Instant t = Instant.parse("2026-08-27T01:00:00Z");
        when(pointQuery.findSeries(anyList(), anyList(), any(), any(), anyString()))
                .thenReturn(List.of(new SeriesPoint(1, "W", 5.0, t)));

        ChartWidgetResponse response = service.getChart(12, null, null, "sum");

        assertThat(response.seriesMode()).isEqualTo("sum");
        assertThat(response.series()).hasSize(1);
        assertThat(response.series().get(0).key()).isEqualTo("sum");
    }

    @Test
    void byPhase_sumsAcrossDevicesPerPoint() {
        DeviceModel model = model(10);
        Device d1 = device(1, "A", "R1", "랙1", model);
        Device d2 = device(2, "B", "R2", "랙2", model);
        PageWidget widget = chartWidget(
                PageWidgetChartScope.devices,
                PageWidgetChartSeriesMode.by_phase,
                List.of("L1", "L2"),
                List.of(d1, d2),
                List.of()
        );
        when(pageWidgetRepository.findById(12)).thenReturn(Optional.of(widget));
        when(deviceModelSnmpPointRepository.findAllEnabledByDeviceModelIds(any())).thenReturn(List.of());
        Instant t = Instant.parse("2026-08-27T01:00:00Z");
        when(pointQuery.findSeries(anyList(), anyList(), any(), any(), anyString()))
                .thenReturn(List.of(
                        new SeriesPoint(1, "L1", 1.0, t),
                        new SeriesPoint(2, "L1", 2.0, t),
                        new SeriesPoint(1, "L2", 3.0, t)
                ));

        ChartWidgetResponse response = service.getChart(12, null, null, null);

        assertThat(response.series()).hasSize(2);
        assertThat(response.series().get(0).key()).isEqualTo("L1");
        assertThat(response.series().get(0).values().get(0)).isEqualTo(3.0);
    }

    @Test
    void modelsScope_usesEnabledDevicesFromRepository() {
        DeviceModel model = model(10);
        Device d1 = device(1, "A", "R1", "랙1", model);
        PageWidget widget = chartWidget(
                PageWidgetChartScope.models,
                PageWidgetChartSeriesMode.sum,
                List.of("W"),
                List.of(),
                List.of(10)
        );
        when(pageWidgetRepository.findById(12)).thenReturn(Optional.of(widget));
        when(deviceRepository.findAllEnabledByDeviceModelIds(List.of(10))).thenReturn(List.of(d1));
        when(deviceModelSnmpPointRepository.findAllEnabledByDeviceModelIds(any())).thenReturn(List.of());
        Instant t = Instant.parse("2026-08-27T01:00:00Z");
        when(pointQuery.findSeries(anyList(), anyList(), any(), any(), anyString()))
                .thenReturn(List.of(new SeriesPoint(1, "W", 5.0, t)));

        ChartWidgetResponse response = service.getChart(12, null, null, null);

        assertThat(response.chartScope()).isEqualTo("models");
        assertThat(response.seriesMode()).isEqualTo("sum");
        assertThat(response.series()).hasSize(1);
        assertThat(response.series().get(0).key()).isEqualTo("sum");
    }

    private static PageWidget chartWidget(
            PageWidgetChartScope scope,
            PageWidgetChartSeriesMode mode,
            List<String> points,
            List<Device> devices,
            List<Integer> modelIds
    ) {
        PageWidget widget = mock(PageWidget.class);
        when(widget.getId()).thenReturn(12);
        when(widget.getName()).thenReturn("차트");
        when(widget.getQueryKind()).thenReturn(PageWidgetQueryKind.chart);
        when(widget.isEnabled()).thenReturn(true);
        when(widget.getChartScope()).thenReturn(scope);
        when(widget.getChartSeriesMode()).thenReturn(mode);
        when(widget.getChartRangePreset()).thenReturn(PageWidgetChartRangePreset.last_24h);
        when(widget.getChartWindow()).thenReturn("5m");
        when(widget.pointNames()).thenReturn(points);
        when(widget.modelIds()).thenReturn(modelIds);
        CommonCode page = pageCode("dashboard");
        when(widget.getPageCode()).thenReturn(page);

        Set<PageWidgetDevice> mappings = new LinkedHashSet<>();
        for (Device device : devices) {
            PageWidgetDevice mapping = mock(PageWidgetDevice.class);
            when(mapping.getDevice()).thenReturn(device);
            mappings.add(mapping);
        }
        when(widget.getDevices()).thenReturn(mappings);
        return widget;
    }

    @Test
    void byPath_mergesDevicesWithSamePathCodeAcrossDifferentParents() {
        CodeGroup pathGroup = CodeGroup.createCodeGroup("LOCATION_PATH", "Location Path");
        CommonCode pathA = CommonCode.createCommonCode(pathGroup, "A", "A Path", 1);

        DeviceModel model = model(10);
        LocationNode building1 = LocationNode.createRoot("BLDG000001", locType(), "빌딩1");
        LocationNode rack1 = LocationNode.createChild("RACK000001", building1, locType(3), "랙1");
        LocationNode building2 = LocationNode.createRoot("BLDG000002", locType(), "빌딩2");
        LocationNode rack2 = LocationNode.createChild("RACK000002", building2, locType(3), "랙2");

        Device d1 = device(1, "PDU-1", rack1, model, pathA);
        Device d2 = device(2, "PDU-2", rack2, model, pathA);
        PageWidget widget = chartWidget(
                PageWidgetChartScope.devices,
                PageWidgetChartSeriesMode.by_path,
                List.of("W"),
                List.of(d1, d2),
                List.of()
        );
        when(pageWidgetRepository.findById(12)).thenReturn(Optional.of(widget));
        when(deviceModelSnmpPointRepository.findAllEnabledByDeviceModelIds(any())).thenReturn(List.of());
        Instant t = Instant.parse("2026-08-27T01:00:00Z");
        when(pointQuery.findSeries(anyList(), anyList(), any(), any(), anyString()))
                .thenReturn(List.of(
                        new SeriesPoint(1, "W", 10.0, t),
                        new SeriesPoint(2, "W", 5.0, t)
                ));

        ChartWidgetResponse response = service.getChart(12, null, null, null);

        assertThat(response.series()).hasSize(1);
        assertThat(response.series().get(0).key()).isEqualTo("A");
        assertThat(response.series().get(0).label()).isEqualTo("A Path");
        assertThat(response.series().get(0).values().get(0)).isEqualTo(15.0);
    }

    private static CommonCode locType() {
        return locType(0);
    }

    private static CommonCode locType(int sortOrder) {
        CodeGroup g = CodeGroup.createCodeGroup("LOCATION_TYPE", "Location Type");
        return CommonCode.createCommonCode(g, "T" + sortOrder, "type", sortOrder);
    }

    private static Device device(int id, String name, LocationNode location, DeviceModel model) {
        return device(id, name, location, model, null);
    }

    private static Device device(
            int id,
            String name,
            LocationNode location,
            DeviceModel model,
            CommonCode pathCode
    ) {
        Device device = mock(Device.class);
        when(device.getId()).thenReturn(id);
        when(device.getName()).thenReturn(name);
        when(device.isEnabled()).thenReturn(true);
        when(device.getDeviceModel()).thenReturn(model);
        when(device.getLocationNode()).thenReturn(location);
        when(device.getPathCode()).thenReturn(pathCode);
        return device;
    }

    private static Device device(int id, String name, String locCode, String locName, DeviceModel model) {
        LocationNode node = mock(LocationNode.class);
        when(node.getCode()).thenReturn(locCode);
        when(node.getName()).thenReturn(locName);
        return device(id, name, node, model);
    }

    private static DeviceModel model(int id) {
        DeviceModel model = mock(DeviceModel.class);
        when(model.getId()).thenReturn(id);
        return model;
    }

    private static CommonCode pageCode(String code) {
        CodeGroup group = mock(CodeGroup.class);
        lenient().when(group.getGroupKey()).thenReturn(DevicePageCodes.DEVICE_PAGE_GROUP_KEY);
        CommonCode page = mock(CommonCode.class);
        when(page.getCode()).thenReturn(code);
        return page;
    }
}
