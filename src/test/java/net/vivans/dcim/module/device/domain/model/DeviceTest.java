package net.vivans.dcim.module.device.domain.model;

import net.vivans.dcim.module.common.domain.model.CodeGroup;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModel;
import net.vivans.dcim.module.location.domain.model.LocationNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeviceTest {

    private static final String RACK_CODE = "R4cK01aB2c";

    @Test
    void create_withValidFields_succeeds() {
        DeviceModel model = DeviceModel.create("AP8959", "APC", modelType(), "3상 PDU");
        LocationNode location = unassignedLocation();

        Device device = Device.create(model, location, "PDU-좌", "Rack 미배정");

        assertThat(device.getId()).isNull();
        assertThat(device.getDeviceModel()).isEqualTo(model);
        assertThat(device.getLocationNode()).isEqualTo(location);
        assertThat(device.getName()).isEqualTo("PDU-좌");
        assertThat(device.getDescription()).isEqualTo("Rack 미배정");
        assertThat(device.isEnabled()).isTrue();
        assertThat(device.isLocationUnassigned()).isTrue();
    }

    @Test
    void create_withEnabledFalse_succeeds() {
        Device device = Device.create(
                DeviceModel.create("LHT65N", "Dragino", modelType(), null),
                unassignedLocation(),
                "센서-01",
                null,
                false
        );

        assertThat(device.isEnabled()).isFalse();
        assertThat(device.getDescription()).isNull();
    }

    @Test
    void create_withoutDeviceModel_throws() {
        assertThatThrownBy(() -> Device.create(null, unassignedLocation(), "PDU-좌", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("deviceModel is required");
    }

    @Test
    void create_withoutLocationNode_throws() {
        DeviceModel model = DeviceModel.create("AP8959", "APC", modelType(), null);

        assertThatThrownBy(() -> Device.create(model, null, "PDU-좌", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("locationNode is required");
    }

    @Test
    void create_withoutName_throws() {
        DeviceModel model = DeviceModel.create("AP8959", "APC", modelType(), null);

        assertThatThrownBy(() -> Device.create(model, unassignedLocation(), "  ", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("name is required");
    }

    @Test
    void update_changesFields() {
        DeviceModel model = DeviceModel.create("AP8959", "APC", modelType(), null);
        LocationNode unassigned = unassignedLocation();
        LocationNode rack = rackLocation();
        Device device = Device.create(model, unassigned, "PDU-좌", null);

        DeviceModel newModel = DeviceModel.create("LHT65N", "Dragino", modelType(), "sensor");
        device.update(newModel, rack, "1층-온습도-01", "배치 완료", false);

        assertThat(device.getDeviceModel()).isEqualTo(newModel);
        assertThat(device.getLocationNode()).isEqualTo(rack);
        assertThat(device.getName()).isEqualTo("1층-온습도-01");
        assertThat(device.getDescription()).isEqualTo("배치 완료");
        assertThat(device.isEnabled()).isFalse();
        assertThat(device.isLocationUnassigned()).isFalse();
    }

    @Test
    void update_withoutName_throws() {
        Device device = Device.create(
                DeviceModel.create("AP8959", "APC", modelType(), null),
                unassignedLocation(),
                "PDU-좌",
                null
        );

        assertThatThrownBy(() -> device.update(
                device.getDeviceModel(),
                device.getLocationNode(),
                null,
                "desc",
                true
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("name is required");
    }

    @Test
    void create_withInvalidPathGroup_throws() {
        DeviceModel model = DeviceModel.create("AP8959", "APC", modelType(), null);
        CodeGroup modelGroup = CodeGroup.createCodeGroup("MODEL_TYPE", "Model");
        CommonCode notPath = CommonCode.createCommonCode(modelGroup, "PDU", "PDU", 1);

        assertThatThrownBy(() -> Device.create(
                model,
                unassignedLocation(),
                "PDU-좌",
                null,
                true,
                notPath
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("pathCode must belong to LOCATION_PATH group");
    }

    @Test
    void create_withLocationPath_succeeds() {
        DeviceModel model = DeviceModel.create("AP8959", "APC", modelType(), null);
        CodeGroup pathGroup = CodeGroup.createCodeGroup("LOCATION_PATH", "Location Path");
        CommonCode pathA = CommonCode.createCommonCode(pathGroup, "A", "A Path", 1);

        Device device = Device.create(model, unassignedLocation(), "PDU-A", null, true, pathA);

        assertThat(device.getPathCode()).isEqualTo(pathA);
    }

    private CommonCode modelType() {
        CodeGroup group = CodeGroup.createCodeGroup("MODEL_TYPE", "Model Type");
        return CommonCode.createCommonCode(group, "PDU", "PDU", 1);
    }

    private LocationNode unassignedLocation() {
        return LocationNode.createRoot(
                Device.UNASSIGNED_LOCATION_CODE,
                locationType("UNASSIGNED", "미배정", -1),
                "미배정"
        );
    }

    private LocationNode rackLocation() {
        return LocationNode.createRoot(
                RACK_CODE,
                locationType("RACK", "랙", 3),
                "Rack-01"
        );
    }

    private CommonCode locationType(String code, String name, int sortOrder) {
        CodeGroup group = CodeGroup.createCodeGroup("LOCATION_TYPE", "Location Type");
        return CommonCode.createCommonCode(group, code, name, sortOrder);
    }
}
