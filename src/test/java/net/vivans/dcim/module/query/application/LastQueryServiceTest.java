package net.vivans.dcim.module.query.application;

import jakarta.persistence.EntityNotFoundException;
import net.vivans.dcim.module.common.domain.model.CodeGroup;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.model.DevicePageCodes;
import net.vivans.dcim.module.device.domain.model.PageWidget;
import net.vivans.dcim.module.device.domain.model.PageWidgetDevice;
import net.vivans.dcim.module.device.domain.model.PageWidgetPoint;
import net.vivans.dcim.module.device.domain.model.PageWidgetQueryKind;
import net.vivans.dcim.module.device.domain.repository.PageWidgetRepository;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModel;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModelSnmpPoint;
import net.vivans.dcim.module.devicemodel.domain.repository.DeviceModelSnmpPointRepository;
import net.vivans.dcim.module.location.domain.model.LocationNode;
import net.vivans.dcim.module.query.api.dto.LastWidgetResponse;
import net.vivans.dcim.module.query.domain.LastPoint;
import net.vivans.dcim.module.query.domain.PointQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LastQueryServiceTest {

    private static final Instant TIME = Instant.parse("2026-08-21T13:51:00Z");

    @Mock
    private PageWidgetRepository pageWidgetRepository;

    @Mock
    private PointQuery pointQuery;

    @Mock
    private DeviceModelSnmpPointRepository deviceModelSnmpPointRepository;

    @InjectMocks
    private LastQueryService service;

    @Test
    void returnsWidgetScopedLastValues() {
        Device first = device(102, "PDU-B", "RACK02", "랙2", "PDU");
        Device second = device(101, "PDU-A", "RACK01", "랙1", "PDU");
        PageWidget widget = lastWidget(12, "dashboard", "칠러", List.of("temp", "V"), List.of(first, second));
        when(pageWidgetRepository.findById(12)).thenReturn(Optional.of(widget));
        when(deviceModelSnmpPointRepository.findAllEnabledByDeviceModelIds(any()))
                .thenReturn(List.of());
        when(pointQuery.findLast(eq(List.of(102, 101)), eq(List.of("temp", "V")), eq(Duration.ofHours(24))))
                .thenReturn(List.of(
                        new LastPoint(102, "temp", 24.1, TIME),
                        new LastPoint(101, "V", 219.0, TIME),
                        new LastPoint(101, "temp", 21.0, TIME)
                ));

        LastWidgetResponse response = service.getLast(12, null);

        assertThat(response.widgetId()).isEqualTo(12);
        assertThat(response.widgetName()).isEqualTo("칠러");
        assertThat(response.pageCode()).isEqualTo("dashboard");
        assertThat(response.devices()).hasSize(2);
        assertThat(response.devices().get(0).deviceId()).isEqualTo(102);
        assertThat(response.devices().get(0).deviceName()).isEqualTo("PDU-B");
        assertThat(response.devices().get(0).points().get(0).unit()).isNull();
        assertThat(response.devices().get(1).deviceId()).isEqualTo(101);
        assertThat(response.devices().get(1).points()).extracting(p -> p.pointName()).containsExactly("V", "temp");
    }

    @Test
    void includesUnitFromDeviceModelSnmpPoint() {
        Device device = device(101, "PDU-A", "RACK01", "랙1", "PDU");
        DeviceModel model = device.getDeviceModel();
        when(model.getId()).thenReturn(55);

        DeviceModelSnmpPoint snmpPoint = mock(DeviceModelSnmpPoint.class);
        var protocol = mock(net.vivans.dcim.module.devicemodel.domain.model.DeviceModelProtocol.class);
        when(snmpPoint.getName()).thenReturn("W");
        when(snmpPoint.getUnit()).thenReturn("W");
        when(snmpPoint.getModelProtocol()).thenReturn(protocol);
        when(protocol.getDeviceModel()).thenReturn(model);

        PageWidget widget = lastWidget(12, "dashboard", "PDU", List.of("W"), List.of(device));
        when(pageWidgetRepository.findById(12)).thenReturn(Optional.of(widget));
        when(deviceModelSnmpPointRepository.findAllEnabledByDeviceModelIds(any()))
                .thenReturn(List.of(snmpPoint));
        when(pointQuery.findLast(eq(List.of(101)), eq(List.of("W")), eq(Duration.ofHours(24))))
                .thenReturn(List.of(new LastPoint(101, "W", 520.0, TIME)));

        LastWidgetResponse response = service.getLast(12, 24);

        assertThat(response.devices()).hasSize(1);
        assertThat(response.devices().get(0).points().get(0).pointName()).isEqualTo("W");
        assertThat(response.devices().get(0).points().get(0).unit()).isEqualTo("W");
        assertThat(response.devices().get(0).points().get(0).value()).isEqualByComparingTo("520.00");
        assertThat(response.total()).isEqualByComparingTo("520.00");
        assertThat(response.totalUnit()).isEqualTo("W");
    }

    @Test
    void skipsDisabledDevices() {
        Device enabled = device(101, "PDU-A", "RACK01", "랙1", "PDU");
        Device disabled = device(102, "PDU-B", "RACK02", "랙2", "PDU");
        when(disabled.isEnabled()).thenReturn(false);
        PageWidget widget = lastWidget(12, "dashboard", "PDU", List.of("W"), List.of(enabled, disabled));
        when(pageWidgetRepository.findById(12)).thenReturn(Optional.of(widget));
        when(deviceModelSnmpPointRepository.findAllEnabledByDeviceModelIds(any()))
                .thenReturn(List.of());
        when(pointQuery.findLast(eq(List.of(101)), eq(List.of("W")), eq(Duration.ofHours(24))))
                .thenReturn(List.of(new LastPoint(101, "W", 10.0, TIME)));

        LastWidgetResponse response = service.getLast(12, 24);

        assertThat(response.devices()).hasSize(1);
        assertThat(response.devices().get(0).deviceId()).isEqualTo(101);
    }

    @Test
    void returnsEmptyDevicesWhenNoPoints() {
        PageWidget widget = lastWidget(12, "dashboard", "빈포인트", List.of(), List.of());
        when(pageWidgetRepository.findById(12)).thenReturn(Optional.of(widget));

        LastWidgetResponse response = service.getLast(12, 24);

        assertThat(response.devices()).isEmpty();
        verify(pointQuery, never()).findLast(any(), any(), any());
    }

    @Test
    void rejectsUnknownWidget() {
        when(pageWidgetRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getLast(99, 24))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("PageWidget not found");
    }

    @Test
    void rejectsDisabledWidget() {
        PageWidget widget = mock(PageWidget.class);
        when(widget.getQueryKind()).thenReturn(PageWidgetQueryKind.last);
        when(widget.isEnabled()).thenReturn(false);
        when(pageWidgetRepository.findById(12)).thenReturn(Optional.of(widget));

        assertThatThrownBy(() -> service.getLast(12, 24))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("widget is disabled");
    }

    @Test
    void rejectsNonLastWidget() {
        PageWidget widget = mock(PageWidget.class);
        when(widget.getQueryKind()).thenReturn(PageWidgetQueryKind.aggregate);
        when(pageWidgetRepository.findById(12)).thenReturn(Optional.of(widget));

        assertThatThrownBy(() -> service.getLast(12, 24))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("queryKind must be last");
    }

    @Test
    void rejectsLookbackOutOfRange() {
        PageWidgetPoint point = mock(PageWidgetPoint.class);
        when(point.getPointName()).thenReturn("W");
        PageWidget widget = mock(PageWidget.class);
        when(widget.getQueryKind()).thenReturn(PageWidgetQueryKind.last);
        when(widget.isEnabled()).thenReturn(true);
        when(widget.getPoints()).thenReturn(List.of(point));
        when(pageWidgetRepository.findById(12)).thenReturn(Optional.of(widget));

        assertThatThrownBy(() -> service.getLast(12, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lookbackHours");
        assertThatThrownBy(() -> service.getLast(12, 169))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lookbackHours");
    }

    private static PageWidget lastWidget(
            int id,
            String pageCode,
            String name,
            List<String> pointNames,
            List<Device> devices
    ) {
        List<PageWidgetPoint> points = new ArrayList<>();
        for (String pointName : pointNames) {
            PageWidgetPoint point = mock(PageWidgetPoint.class);
            when(point.getPointName()).thenReturn(pointName);
            points.add(point);
        }

        Set<PageWidgetDevice> mappings = new LinkedHashSet<>();
        for (Device device : devices) {
            PageWidgetDevice mapping = mock(PageWidgetDevice.class);
            when(mapping.getDevice()).thenReturn(device);
            mappings.add(mapping);
        }

        PageWidget widget = mock(PageWidget.class);
        when(widget.getId()).thenReturn(id);
        when(widget.getName()).thenReturn(name);
        when(widget.isEnabled()).thenReturn(true);
        when(widget.getPageCode()).thenReturn(pageCode(pageCode));
        when(widget.getQueryKind()).thenReturn(PageWidgetQueryKind.last);
        when(widget.getPoints()).thenReturn(points);
        lenient().when(widget.getDevices()).thenReturn(mappings);
        return widget;
    }

    private static CommonCode pageCode(String code) {
        CodeGroup group = CodeGroup.createCodeGroup(DevicePageCodes.DEVICE_PAGE_GROUP_KEY, "Device Page");
        return CommonCode.createCommonCode(group, code, code, 1);
    }

    private static Device device(
            int id,
            String name,
            String locationCode,
            String locationName,
            String deviceTypeCode
    ) {
        Device device = mock(Device.class);
        LocationNode location = mock(LocationNode.class);
        DeviceModel model = mock(DeviceModel.class);
        CommonCode deviceType = mock(CommonCode.class);
        lenient().when(device.getId()).thenReturn(id);
        lenient().when(device.getName()).thenReturn(name);
        when(device.isEnabled()).thenReturn(true);
        lenient().when(device.getLocationNode()).thenReturn(location);
        lenient().when(location.getCode()).thenReturn(locationCode);
        lenient().when(location.getName()).thenReturn(locationName);
        lenient().when(device.getDeviceModel()).thenReturn(model);
        lenient().when(model.getId()).thenReturn(id);
        lenient().when(model.getDeviceType()).thenReturn(deviceType);
        lenient().when(deviceType.getCode()).thenReturn(deviceTypeCode);
        return device;
    }
}
