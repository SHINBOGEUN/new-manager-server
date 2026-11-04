package net.vivans.dcim.module.device.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SceneModelTest {
    @Test
    void uploadedModelCanBeUpdatedAndReplaced() {
        SceneModel model = SceneModel.uploaded("USR_123456789012345678901234", "랙", "랙·서버", "first.glb", "rack.glb", 1, 2, 1);
        assertTrue(model.isEnabled());
        assertFalse(model.isBuiltin());
        model.update("냉각 랙", "냉각 설비", 1.2, 2.4, 1.1, false);
        model.replaceFile("second.glb", "cooler.glb");
        assertEquals("냉각 랙", model.getName());
        assertEquals("냉각 설비", model.getCategory());
        assertEquals("second.glb", model.getStorageKey());
        assertFalse(model.isEnabled());
    }

    @Test
    void invalidDimensionsAndNamesAreRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                SceneModel.uploaded("USR_123456789012345678901234", " ", "기타", "file.glb", "file.glb", 1, 2, 1));
        assertThrows(IllegalArgumentException.class, () ->
                SceneModel.uploaded("USR_123456789012345678901234", "랙", "기타", "file.glb", "file.glb", Double.NaN, 2, 1));
        assertThrows(IllegalArgumentException.class, () ->
                SceneModel.uploaded("USR_123456789012345678901234", "랙", " ", "file.glb", "file.glb", 1, 2, 1));
    }
    @Test
    void rackInstallationModesAreIndependent() {
        SceneModel model = SceneModel.uploaded("USR_123456789012345678901234", "센서", "센서", "sensor.glb", "sensor.glb", .068, .12, .027);
        model.setRackMountOptions(false, true);
        assertTrue(model.permitsRackMount(DeviceMountType.RACK_SIDE));
        assertTrue(model.permitsRackMount(DeviceMountType.RACK_REAR));
        assertFalse(model.permitsRackMount(DeviceMountType.RACK_U));
        model.setRackMountOptions(true, false);
        assertTrue(model.permitsRackMount(DeviceMountType.RACK_U));
        assertFalse(model.permitsRackMount(DeviceMountType.RACK_SIDE));
        model.setRackMountOptions(false, false);
        assertFalse(model.isRackInstallable());
    }

}
