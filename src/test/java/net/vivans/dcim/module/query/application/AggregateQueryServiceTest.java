package net.vivans.dcim.module.query.application;

import jakarta.persistence.EntityNotFoundException;
import net.vivans.dcim.module.common.domain.model.CodeGroup;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.model.DevicePageCodes;
import net.vivans.dcim.module.device.domain.model.PageWidget;
import net.vivans.dcim.module.device.domain.model.PageWidgetChartRangePreset;
import net.vivans.dcim.module.device.domain.model.PageWidgetDevice;
import net.vivans.dcim.module.device.domain.model.PageWidgetDeviceRole;
import net.vivans.dcim.module.device.domain.model.PageWidgetOp;
import net.vivans.dcim.module.device.domain.model.PageWidgetQueryKind;
import net.vivans.dcim.module.device.domain.repository.PageWidgetRepository;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModel;
import net.vivans.dcim.module.devicemodel.domain.repository.DeviceModelSnmpPointRepository;
import net.vivans.dcim.module.query.api.dto.AggregateWidgetResponse;
import net.vivans.dcim.module.query.domain.LastPoint;
import net.vivans.dcim.module.query.domain.PointQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AggregateQueryServiceTest {

    private static final Instant T0 = Instant.parse("2026-08-31T00:00:00Z");
    private static final Instant T1 = Instant.parse("2026-08-31T12:00:00Z");

    @Mock
    private PageWidgetRepository pageWidgetRepository;

    @Mock
    private PointQuery pointQuery;

    @Mock
    private DeviceModelSnmpPointRepository deviceModelSnmpPointRepository;

    @InjectMocks
    private AggregateQueryService service;

    @Test
    void usageSumsLastMinusFirstPerDevice() {
        Device d1 = device(101);
        Device d2 = device(102);
        PageWidget widget = aggregateWidget(
                12, "오늘 kWh", PageWidgetOp.usage, PageWidgetChartRangePreset.today,
                List.of(mapping(d1, PageWidgetDeviceRole.DEFAULT), mapping(d2, PageWidgetDeviceRole.DEFAULT)),
                "TOTAL_KWH");
        when(pageWidgetRepository.findById(12)).thenReturn(Optional.of(widget));
        when(deviceModelSnmpPointRepository.findAllEnabledByDeviceModelIds(any())).thenReturn(List.of());
        when(pointQuery.findFirstInRange(eq(List.of(101, 102)), eq(List.of("TOTAL_KWH")), any(), any()))
                .thenReturn(List.of(
                        new LastPoint(101, "TOTAL_KWH", 100.0, T0),
                        new LastPoint(102, "TOTAL_KWH", 200.0, T0)
                ));
        when(pointQuery.findLastInRange(eq(List.of(101, 102)), eq(List.of("TOTAL_KWH")), any(), any()))
                .thenReturn(List.of(
                        new LastPoint(101, "TOTAL_KWH", 150.0, T1),
                        new LastPoint(102, "TOTAL_KWH", 260.0, T1)
                ));

        AggregateWidgetResponse response = service.getAggregate(12, "today");

        assertThat(response.aggregatePreset()).isEqualTo("usage");
        assertThat(response.rangePreset()).isEqualTo("today");
        assertThat(response.value()).isEqualByComparingTo("110.00");
        assertThat(response.contributingDevices()).isEqualTo(2);
        assertThat(response.devices()).hasSize(2);
        assertThat(response.devices()).extracting(d -> d.deviceId()).containsExactly(101, 102);
        assertThat(response.devices()).extracting(d -> d.value()).containsExactly(
                new java.math.BigDecimal("50.00"),
                new java.math.BigDecimal("60.00")
        );
    }

    @Test
    void usageSkipsNegativeDelta() {
        Device d1 = device(101);
        PageWidget widget = aggregateWidget(
                12, "kWh", PageWidgetOp.usage, PageWidgetChartRangePreset.today,
                List.of(mapping(d1, PageWidgetDeviceRole.DEFAULT)),
                "TOTAL_KWH");
        when(pageWidgetRepository.findById(12)).thenReturn(Optional.of(widget));
        when(deviceModelSnmpPointRepository.findAllEnabledByDeviceModelIds(any())).thenReturn(List.of());
        when(pointQuery.findFirstInRange(anyList(), anyList(), any(), any()))
                .thenReturn(List.of(new LastPoint(101, "TOTAL_KWH", 100.0, T0)));
        when(pointQuery.findLastInRange(anyList(), anyList(), any(), any()))
                .thenReturn(List.of(new LastPoint(101, "TOTAL_KWH", 50.0, T1)));

        AggregateWidgetResponse response = service.getAggregate(12, null);

        assertThat(response.value()).isNull();
        assertThat(response.contributingDevices()).isZero();
        assertThat(response.devices()).isEmpty();
    }

    @Test
    void powerSumsLastW() {
        Device d1 = device(101);
        Device d2 = device(102);
        PageWidget widget = aggregateWidget(
                12, "IT Power", PageWidgetOp.power, PageWidgetChartRangePreset.last_24h,
                List.of(mapping(d1, PageWidgetDeviceRole.DEFAULT), mapping(d2, PageWidgetDeviceRole.DEFAULT)),
                "TOTAL_WT");
        when(pageWidgetRepository.findById(12)).thenReturn(Optional.of(widget));
        when(deviceModelSnmpPointRepository.findAllEnabledByDeviceModelIds(any())).thenReturn(List.of());
        when(pointQuery.findLastInRange(eq(List.of(101, 102)), eq(List.of("TOTAL_WT")), any(), any()))
                .thenReturn(List.of(
                        new LastPoint(101, "TOTAL_WT", 200.0, T1),
                        new LastPoint(102, "TOTAL_WT", 100.0, T1)
                ));

        AggregateWidgetResponse response = service.getAggregate(12, null);

        assertThat(response.aggregatePreset()).isEqualTo("power");
        assertThat(response.rangePreset()).isEqualTo("last_24h");
        assertThat(response.value()).isEqualByComparingTo("300.00");
        assertThat(response.contributingDevices()).isEqualTo(2);
        assertThat(response.devices()).extracting(d -> d.value()).containsExactly(
                new java.math.BigDecimal("200.00"),
                new java.math.BigDecimal("100.00")
        );
    }

    @Test
    void defaultsUsageRangePresetToToday() {
        Device d1 = device(101);
        PageWidget widget = aggregateWidget(
                12, "kWh", PageWidgetOp.usage, null,
                List.of(mapping(d1, PageWidgetDeviceRole.DEFAULT)),
                "TOTAL_KWH");
        when(pageWidgetRepository.findById(12)).thenReturn(Optional.of(widget));
        when(deviceModelSnmpPointRepository.findAllEnabledByDeviceModelIds(any())).thenReturn(List.of());
        when(pointQuery.findFirstInRange(anyList(), anyList(), any(), any())).thenReturn(List.of());
        when(pointQuery.findLastInRange(anyList(), anyList(), any(), any())).thenReturn(List.of());

        AggregateWidgetResponse response = service.getAggregate(12, null);

        assertThat(response.rangePreset()).isEqualTo("today");
        verify(pointQuery).findFirstInRange(
                anyList(),
                anyList(),
                ArgumentMatchers.any(Instant.class),
                ArgumentMatchers.any(Instant.class)
        );
    }

    @Test
    void rejectsNonAggregateWidget() {
        PageWidget widget = mock(PageWidget.class);
        when(widget.getQueryKind()).thenReturn(PageWidgetQueryKind.last);
        when(pageWidgetRepository.findById(12)).thenReturn(Optional.of(widget));

        assertThatThrownBy(() -> service.getAggregate(12, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("queryKind must be aggregate");
    }

    @Test
    void rejectsMissingWidget() {
        when(pageWidgetRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAggregate(99, null))
                .isInstanceOf(EntityNotFoundException.class);
    }

    private static PageWidget aggregateWidget(
            int id,
            String name,
            PageWidgetOp op,
            PageWidgetChartRangePreset rangePreset,
            List<PageWidgetDevice> mappings,
            String pointName
    ) {
        Set<PageWidgetDevice> deviceSet = new LinkedHashSet<>(mappings);
        PageWidget widget = mock(PageWidget.class);
        when(widget.getId()).thenReturn(id);
        when(widget.getName()).thenReturn(name);
        when(widget.isEnabled()).thenReturn(true);
        when(widget.getPageCode()).thenReturn(pageCode("dashboard"));
        when(widget.getQueryKind()).thenReturn(PageWidgetQueryKind.aggregate);
        when(widget.getOp()).thenReturn(op);
        lenient().when(widget.getAggregateRangePreset()).thenReturn(rangePreset);
        when(widget.getDevices()).thenReturn(deviceSet);
        when(widget.pointNames()).thenReturn(List.of(pointName));
        return widget;
    }

    private static PageWidgetDevice mapping(Device device, PageWidgetDeviceRole role) {
        PageWidgetDevice mapping = mock(PageWidgetDevice.class);
        when(mapping.getDevice()).thenReturn(device);
        when(mapping.getDeviceRole()).thenReturn(role);
        return mapping;
    }

    private static CommonCode pageCode(String code) {
        CodeGroup group = CodeGroup.createCodeGroup(DevicePageCodes.DEVICE_PAGE_GROUP_KEY, "Device Page");
        return CommonCode.createCommonCode(group, code, code, 1);
    }

    private static Device device(int id) {
        Device device = mock(Device.class);
        DeviceModel model = mock(DeviceModel.class);
        when(device.getId()).thenReturn(id);
        lenient().when(device.getName()).thenReturn("D-" + id);
        when(device.isEnabled()).thenReturn(true);
        lenient().when(device.getDeviceModel()).thenReturn(model);
        lenient().when(model.getId()).thenReturn(1);
        return device;
    }
}
