package net.vivans.dcim.module.device.domain.model;

import net.vivans.dcim.module.common.domain.model.CodeGroup;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageWidgetTest {

    @Test
    void create_withDevicePageCode_succeeds() {
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
                List.of("status", "W")
        );

        assertThat(widget.getName()).isEqualTo("칠러");
        assertThat(widget.getQueryKind()).isEqualTo(PageWidgetQueryKind.last);
        assertThat(widget.isEnabled()).isTrue();
        assertThat(widget.pointNames()).containsExactly("status", "W");
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
                null, null, null, null, null, null
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
                List.of("TOTAL_KWH")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("op is required for aggregate");
    }

    @Test
    void update_replacesPoints() {
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
                List.of("W")
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
                List.of("status", "W")
        );

        assertThat(widget.getName()).isEqualTo("칠러 상태");
        assertThat(widget.isEnabled()).isFalse();
        assertThat(widget.pointNames()).containsExactly("status", "W");
    }

    @Test
    void queryKindFrom_rejectsUnknown() {
        assertThatThrownBy(() -> PageWidgetQueryKind.from("chart"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("queryKind must be last, aggregate, or count");
    }

    private CommonCode pageCode(String code, String name) {
        CodeGroup group = CodeGroup.createCodeGroup(DevicePage.DEVICE_PAGE_GROUP_KEY, "Device Page");
        return CommonCode.createCommonCode(group, code, name, 1);
    }
}
