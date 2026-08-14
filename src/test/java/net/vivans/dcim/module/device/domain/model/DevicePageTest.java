package net.vivans.dcim.module.device.domain.model;

import net.vivans.dcim.module.common.domain.model.CodeGroup;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModel;
import net.vivans.dcim.module.location.domain.model.LocationNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DevicePageTest {

    @Test
    void create_withDevicePageCode_succeeds() {
        DevicePage devicePage = DevicePage.create(device(), pageCode("ENVIRONMENT", "Environment"));

        assertThat(devicePage.getDevice()).isNotNull();
        assertThat(devicePage.getPageCode().getCode()).isEqualTo("ENVIRONMENT");
    }

    @Test
    void create_withNonDevicePageGroup_throws() {
        CommonCode wrong = CommonCode.createCommonCode(
                CodeGroup.createCodeGroup("PROTOCOL_TYPE", "Protocol Type"),
                "snmp",
                "SNMP",
                1
        );

        assertThatThrownBy(() -> DevicePage.create(device(), wrong))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("pageCode must belong to DEVICE_PAGE group");
    }

    private Device device() {
        return Device.create(
                DeviceModel.create("AP8959", "APC", modelType(), null),
                LocationNode.createRoot(
                        Device.UNASSIGNED_LOCATION_CODE,
                        locationType(),
                        "미배정"
                ),
                "PDU-좌",
                null
        );
    }

    private CommonCode pageCode(String code, String name) {
        CodeGroup group = CodeGroup.createCodeGroup(DevicePage.DEVICE_PAGE_GROUP_KEY, "Device Page");
        return CommonCode.createCommonCode(group, code, name, 1);
    }

    private CommonCode modelType() {
        CodeGroup group = CodeGroup.createCodeGroup("MODEL_TYPE", "Model Type");
        return CommonCode.createCommonCode(group, "PDU", "PDU", 1);
    }

    private CommonCode locationType() {
        CodeGroup group = CodeGroup.createCodeGroup("LOCATION_TYPE", "Location Type");
        return CommonCode.createCommonCode(group, "UNASSIGNED", "미배정", -1);
    }
}
