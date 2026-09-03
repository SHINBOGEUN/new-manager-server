package net.vivans.dcim.module.device.domain.model;

import net.vivans.dcim.module.common.domain.model.CodeGroup;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PageWidgetTest {

    @Test
    void create_withDevicesAndPoints_succeeds() {
        PageWidget widget = lastWidget("칠러", List.of("status", "W"), List.of(device(9), device(10)));

        assertThat(widget.getName()).isEqualTo("칠러");
        assertThat(widget.getQueryKind()).isEqualTo(PageWidgetQueryKind.last);
        assertThat(widget.isEnabled()).isTrue();
        assertThat(widget.pointNames()).containsExactly("status", "W");
        assertThat(widget.deviceIds()).containsExactly(9, 10);
        assertThat(widget.getLayout()).isNull();
    }

    @Test
    void upsertLayout_setsAndUpdatesGrid() {
        PageWidget widget = lastWidget("PDU", List.of("W"), List.of(device(1)));

        widget.upsertLayout(1, 2, 3, 1);
        assertThat(widget.getLayout().getGridX()).isEqualTo(1);
        assertThat(widget.getLayout().getGridY()).isEqualTo(2);
        assertThat(widget.getLayout().getW()).isEqualTo(3);
        assertThat(widget.getLayout().getH()).isEqualTo(1);

        widget.upsertLayout(0, 0, 2, 2);
        assertThat(widget.getLayout().getW()).isEqualTo(2);
        assertThat(widget.getLayout().getH()).isEqualTo(2);
    }

    @Test
    void upsertLayout_withInvalidSize_throws() {
        PageWidget widget = lastWidget("PDU", List.of("W"), List.of(device(1)));

        assertThatThrownBy(() -> widget.upsertLayout(0, 0, 0, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("w must be >= 1");
    }

    @Test
    void create_count_clearsDevicesEvenIfPassed() {
        PageWidget widget = PageWidget.create(
                pageCode("dashboard", "dashboard"),
                "장비 수",
                true,
                PageWidgetQueryKind.count,
                null, null, null,
                PageWidgetCountMode.total,
                null,
                null, null, null, null,
                List.of("W"),
                List.of(device(1), device(2)),
                List.of(),
                List.of()
        );

        assertThat(widget.deviceIds()).isEmpty();
        assertThat(widget.pointNames()).isEmpty();
        assertThat(widget.getCountMode()).isEqualTo(PageWidgetCountMode.total);
    }

    @Test
    void create_countWithoutDevices_succeeds() {
        PageWidget widget = PageWidget.create(
                pageCode("dashboard", "dashboard"),
                "장비 수",
                true,
                PageWidgetQueryKind.count,
                null, null, null,
                PageWidgetCountMode.total,
                null,
                null, null, null, null,
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );

        assertThat(widget.getQueryKind()).isEqualTo(PageWidgetQueryKind.count);
        assertThat(widget.getCountMode()).isEqualTo(PageWidgetCountMode.total);
        assertThat(widget.deviceIds()).isEmpty();
    }

    @Test
    void create_chartWithDevices_succeeds() {
        PageWidget widget = PageWidget.create(
                pageCode("dashboard", "dashboard"),
                "전력 차트",
                true,
                PageWidgetQueryKind.chart,
                null, null, null,
                null, null,
                PageWidgetChartScope.devices,
                PageWidgetChartSeriesMode.per_device,
                PageWidgetChartRangePreset.last_24h,
                "5m",
                List.of("W"),
                List.of(device(1), device(2)),
                List.of(),
                List.of()
        );

        assertThat(widget.getQueryKind()).isEqualTo(PageWidgetQueryKind.chart);
        assertThat(widget.getChartScope()).isEqualTo(PageWidgetChartScope.devices);
        assertThat(widget.getChartSeriesMode()).isEqualTo(PageWidgetChartSeriesMode.per_device);
        assertThat(widget.deviceIds()).containsExactly(1, 2);
        assertThat(widget.modelIds()).isEmpty();
        assertThat(widget.pointNames()).containsExactly("W");
    }

    @Test
    void create_chartWithModels_succeeds() {
        PageWidget widget = PageWidget.create(
                pageCode("dashboard", "dashboard"),
                "모델 차트",
                true,
                PageWidgetQueryKind.chart,
                null, null, null,
                null, null,
                PageWidgetChartScope.models,
                PageWidgetChartSeriesMode.by_phase,
                PageWidgetChartRangePreset.this_month,
                "1h",
                List.of("L1", "L2", "L3"),
                List.of(),
                List.of(),
                List.of(10, 20)
        );

        assertThat(widget.getChartScope()).isEqualTo(PageWidgetChartScope.models);
        assertThat(widget.modelIds()).containsExactly(10, 20);
        assertThat(widget.deviceIds()).isEmpty();
        assertThat(widget.pointNames()).containsExactly("L1", "L2", "L3");
    }

    @Test
    void update_chart_keepsSameChartRow() {
        PageWidget widget = PageWidget.create(
                pageCode("dashboard", "dashboard"),
                "전력 차트",
                true,
                PageWidgetQueryKind.chart,
                null, null, null,
                null, null,
                PageWidgetChartScope.devices,
                PageWidgetChartSeriesMode.sum,
                PageWidgetChartRangePreset.last_24h,
                "15m",
                List.of("W"),
                List.of(device(1)),
                List.of(),
                List.of()
        );

        widget.update(
                "전력 차트",
                true,
                PageWidgetQueryKind.chart,
                null, null, null,
                null, null,
                PageWidgetChartScope.devices,
                PageWidgetChartSeriesMode.by_phase,
                PageWidgetChartRangePreset.last_24h,
                "15m",
                List.of("L1_WATT", "L2_WATT", "L3_WATT"),
                List.of(device(1), device(2)),
                List.of(),
                List.of()
        );

        assertThat(widget.getChartSeriesMode()).isEqualTo(PageWidgetChartSeriesMode.by_phase);
        assertThat(widget.deviceIds()).containsExactly(1, 2);
    }

    @Test
    void create_withoutDevices_throws() {
        assertThatThrownBy(() -> lastWidget("칠러", List.of("W"), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("deviceIds is required");
    }

    @Test
    void create_withNonDevicePageGroup_throws() {
        CommonCode wrong = CommonCode.createCommonCode(
                CodeGroup.createCodeGroup("PROTOCOL_TYPE", "Protocol Type"),
                "snmp",
                "SNMP",
                1
        );

        assertThatThrownBy(() -> PageWidget.create(
                wrong, "칠러", true, PageWidgetQueryKind.last,
                null, null, null, null, null,
                null, null, null, null,
                List.of("W"),
                List.of(device(1)),
                List.of(),
                List.of()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("pageCode must belong to DEVICE_PAGE group");
    }

    @Test
    void create_aggregateWithoutOp_throws() {
        assertThatThrownBy(() -> PageWidget.create(
                pageCode("dashboard", "dashboard"),
                "오늘 kWh",
                true,
                PageWidgetQueryKind.aggregate,
                null, null, null, null, null,
                null, null, null, null,
                List.of(),
                List.of(device(1)),
                List.of(),
                List.of()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("aggregatePreset/op is required for aggregate");
    }

    @Test
    void create_usageRequiresPointName() {
        PageWidget widget = PageWidget.create(
                pageCode("dashboard", "dashboard"),
                "오늘 사용량",
                true,
                PageWidgetQueryKind.aggregate,
                PageWidgetOp.usage, null, null,
                null, null,
                null, null, null, null,
                List.of("TOTAL_KWH"),
                List.of(device(1)),
                List.of(),
                List.of()
        );

        assertThat(widget.getOp()).isEqualTo(PageWidgetOp.usage);
        assertThat(widget.getAggregateRangePreset()).isEqualTo(PageWidgetChartRangePreset.today);
        assertThat(widget.pointNames()).containsExactly("TOTAL_KWH");
        assertThat(widget.deviceIds()).containsExactly(1);
    }

    @Test
    void create_aggregateWithoutPointName_throws() {
        assertThatThrownBy(() -> PageWidget.create(
                pageCode("dashboard", "dashboard"),
                "전력",
                true,
                PageWidgetQueryKind.aggregate,
                PageWidgetOp.power, null, PageWidgetChartRangePreset.today,
                null, null,
                null, null, null, null,
                List.of(),
                List.of(device(1)),
                List.of(),
                List.of()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("pointNames is required for aggregate");
    }

    @Test
    void update_replacesDevicesAndPoints() {
        PageWidget widget = lastWidget("칠러", List.of("W"), List.of(device(9)));

        widget.update(
                "칠러 상태",
                false,
                PageWidgetQueryKind.last,
                null, null, null,
                null, null,
                null, null, null, null,
                List.of("status", "W"),
                List.of(device(11)),
                List.of(),
                List.of()
        );

        assertThat(widget.getName()).isEqualTo("칠러 상태");
        assertThat(widget.isEnabled()).isFalse();
        assertThat(widget.pointNames()).containsExactly("status", "W");
        assertThat(widget.deviceIds()).containsExactly(11);
    }

    @Test
    void queryKindFrom_rejectsUnknown() {
        assertThatThrownBy(() -> PageWidgetQueryKind.from("gauge"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("queryKind must be last, aggregate, count, or chart");
    }

    private static PageWidget lastWidget(String name, List<String> points, List<Device> devices) {
        return PageWidget.create(
                pageCode("dashboard", "dashboard"),
                name,
                true,
                PageWidgetQueryKind.last,
                null, null, null,
                null, null,
                null, null, null, null,
                points,
                devices,
                List.of(),
                List.of()
        );
    }

    private static CommonCode pageCode(String code, String name) {
        CodeGroup group = CodeGroup.createCodeGroup(DevicePageCodes.DEVICE_PAGE_GROUP_KEY, "Device Page");
        return CommonCode.createCommonCode(group, code, name, 1);
    }

    private static Device device(int id) {
        Device device = mock(Device.class);
        when(device.getId()).thenReturn(id);
        return device;
    }
}
