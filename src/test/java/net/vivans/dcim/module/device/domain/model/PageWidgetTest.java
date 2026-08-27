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
        PageWidget widget = PageWidget.create(
                pageCode("cooling", "Cooling"),
                "칠러",
                true,
                PageWidgetQueryKind.last,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of("status", "W"),
                List.of(device(9), device(10))
        );

        assertThat(widget.getName()).isEqualTo("칠러");
        assertThat(widget.getQueryKind()).isEqualTo(PageWidgetQueryKind.last);
        assertThat(widget.isEnabled()).isTrue();
        assertThat(widget.pointNames()).containsExactly("status", "W");
        assertThat(widget.deviceIds()).containsExactly(9, 10);
        assertThat(widget.getLayout()).isNull();
    }

    @Test
    void upsertLayout_setsAndUpdatesGrid() {
        PageWidget widget = PageWidget.create(
                pageCode("dashboard", "dashboard"),
                "PDU",
                true,
                PageWidgetQueryKind.last,
                null, null, null, null, null, null, null,
                List.of("W"),
                List.of(device(1))
        );

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
        PageWidget widget = PageWidget.create(
                pageCode("dashboard", "dashboard"),
                "PDU",
                true,
                PageWidgetQueryKind.last,
                null, null, null, null, null, null, null,
                List.of("W"),
                List.of(device(1))
        );

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
                null, null, null, null, null,
                PageWidgetCountMode.total,
                null,
                List.of("W"),
                List.of(device(1), device(2))
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
                null, null, null, null, null,
                PageWidgetCountMode.total,
                null,
                List.of(),
                List.of()
        );

        assertThat(widget.getQueryKind()).isEqualTo(PageWidgetQueryKind.count);
        assertThat(widget.getCountMode()).isEqualTo(PageWidgetCountMode.total);
        assertThat(widget.deviceIds()).isEmpty();
    }

    @Test
    void create_withoutDevices_throws() {
        assertThatThrownBy(() -> PageWidget.create(
                pageCode("cooling", "Cooling"),
                "칠러",
                true,
                PageWidgetQueryKind.last,
                null, null, null, null, null, null, null,
                List.of("W"),
                List.of()
        ))
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
                null, null, null, null, null, null, null,
                List.of("W"),
                List.of(device(1))
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
                null, null, null, null, null, null, null,
                List.of("TOTAL_KWH"),
                List.of(device(1))
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("op is required for aggregate");
    }

    @Test
    void update_replacesDevicesAndPoints() {
        PageWidget widget = PageWidget.create(
                pageCode("dashboard", "dashboard"),
                "칠러",
                true,
                PageWidgetQueryKind.last,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of("W"),
                List.of(device(9))
        );

        widget.update(
                "칠러 상태",
                false,
                PageWidgetQueryKind.last,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of("status", "W"),
                List.of(device(11))
        );

        assertThat(widget.getName()).isEqualTo("칠러 상태");
        assertThat(widget.isEnabled()).isFalse();
        assertThat(widget.pointNames()).containsExactly("status", "W");
        assertThat(widget.deviceIds()).containsExactly(11);
    }

    @Test
    void queryKindFrom_rejectsUnknown() {
        assertThatThrownBy(() -> PageWidgetQueryKind.from("chart"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("queryKind must be last, aggregate, or count");
    }

    private CommonCode pageCode(String code, String name) {
        CodeGroup group = CodeGroup.createCodeGroup(DevicePageCodes.DEVICE_PAGE_GROUP_KEY, "Device Page");
        return CommonCode.createCommonCode(group, code, name, 1);
    }

    private static Device device(int id) {
        Device device = mock(Device.class);
        when(device.getId()).thenReturn(id);
        return device;
    }
}
