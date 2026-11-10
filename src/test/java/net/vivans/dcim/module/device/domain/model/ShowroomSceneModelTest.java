package net.vivans.dcim.module.device.domain.model;

import org.junit.jupiter.api.Test;
import net.vivans.dcim.module.location.domain.model.LocationNode;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.common.domain.model.CodeGroup;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ShowroomSceneModelTest {
    /** 카탈로그 scene_role 시드 값과 같다(sql/migration/20261110_scene_model_role.sql). */
    private static SceneModelRole roleFor(String key) {
        return switch (key) {
            case "SR_RACK", "SR_DOUBLE_RACK", "SR_DLC_RACK", "SR_INROW_RACK", "SR_OCP_RACK", "SR_RDC_RACK",
                 "SR_ORV3_RACK", "SR_HAC_RACK" -> SceneModelRole.RACK;
            case "SR_INROW_PANEL" -> SceneModelRole.PANEL;
            case "SR_INROW_MOLD", "SR_WALL_COIL" -> SceneModelRole.FRAME;
            default -> null;
        };
    }

    @Test void roleComesFromCatalogNotFromTheModelKeyName() {
        DevicePageSceneItem renamedRack = new DevicePageSceneItem();
        renamedRack.update(null, null, null, "EQUIPMENT", null, "Rack", "USR_ABCDEFGHIJKLMNOPQRSTUVWX", SceneModelRole.RACK, 0, 0, 0, 0, 4, 4, 4);
        assertDoesNotThrow(() -> renamedRack.setRackColor("#123456"));
        DevicePageSceneItem keyLooksLikeRack = new DevicePageSceneItem();
        keyLooksLikeRack.update(null, null, null, "EQUIPMENT", null, "Rack", "SR_RACK", null, 0, 0, 0, 0, 4, 4, 4);
        assertThrows(IllegalArgumentException.class, () -> keyLooksLikeRack.setRackColor("#123456"));
        DevicePageSceneItem primitive = new DevicePageSceneItem();
        assertThrows(IllegalArgumentException.class,
                () -> primitive.update(null, null, null, "EQUIPMENT", null, "Floor", "FLOOR", SceneModelRole.RACK, 0, 0, 0, 0, 4, 4, 4));
        assertThrows(IllegalArgumentException.class,
                () -> primitive.update(null, null, null, "EQUIPMENT", null, "Bad", "bad key", null, 0, 0, 0, 0, 4, 4, 4));
    }

    @Test void panelPhotoAndLayoutRequirePanelRole() {
        DevicePageSceneItem panel = new DevicePageSceneItem();
        panel.update(null, null, null, "EQUIPMENT", null, "Panel", "SR_INROW_PANEL", SceneModelRole.PANEL, 0, 0, 0, 0, 4, 4, 4);
        assertDoesNotThrow(() -> panel.setPanelLayoutInitialized(true));
        DevicePageSceneItem rack = new DevicePageSceneItem();
        rack.update(null, null, null, "EQUIPMENT", null, "Rack", "SR_RACK", SceneModelRole.RACK, 0, 0, 0, 0, 4, 4, 4);
        assertThrows(IllegalArgumentException.class, () -> rack.setPanelLayoutInitialized(true));
    }

    @Test void rackColorAcceptsHexAndRejectsOtherEquipment() {
        DevicePageSceneItem rack = new DevicePageSceneItem();
        rack.update(null, null, null, "EQUIPMENT", null, "Rack", "SR_RACK", SceneModelRole.RACK, 0, 0, 0, 0, 4, 4, 4);
        rack.setRackColor("#a1b2c3"); assertEquals("#A1B2C3", rack.getRackColor());
        assertThrows(IllegalArgumentException.class, () -> rack.setRackColor("not-a-color"));
        rack.setRackColor(null); assertNull(rack.getRackColor());
        DevicePageSceneItem pdu = new DevicePageSceneItem();
        pdu.update(null, null, null, "EQUIPMENT", null, "PDU", "SR_PDU_RACK", null, 0, 0, 0, 0, 4, 4, 4);
        assertThrows(IllegalArgumentException.class, () -> pdu.setRackColor("#FFFFFF"));
    }
    @Test void showroomRackCanReferenceRackLocationButOtherEquipmentCannot() {
        CommonCode type = CommonCode.createCommonCode(CodeGroup.createCodeGroup("LOCATION_TYPE", "위치"), "RACK", "랙", 1);
        LocationNode location = LocationNode.createRoot("0000000001", type, "랙", 42);
        DevicePageSceneItem item = new DevicePageSceneItem();
        assertDoesNotThrow(() -> item.update(null, null, location, "EQUIPMENT", null, "Rack", "SR_RACK", SceneModelRole.RACK, 0, 0, 0, 0, 4, 4, 4));
        assertThrows(IllegalArgumentException.class, () -> item.update(null, null, location, "EQUIPMENT", null, "CDU", "SR_DLC_CDU", null, 0, 0, 0, 0, 4, 4, 4));
        assertThrows(IllegalArgumentException.class, () -> item.update(null, null, location, "EQUIPMENT", null, "PDU", "SR_PDU_RACK", null, 0, 0, 0, 0, 4, 4, 4));
    }
    @Test void equipmentLockCanBeSetAndCleared() {
        DevicePageSceneItem item = new DevicePageSceneItem();
        item.update(null, null, null, "EQUIPMENT", null, "Rack", "SR_RACK", SceneModelRole.RACK, 0, 0, 0, 0, 12, 12, 12);
        item.setLocked(true); assertTrue(item.isLocked());
        item.setLocked(false); assertFalse(item.isLocked());
    }
    @Test void equipmentGroupKeyIsValidated() {
        DevicePageSceneItem item = new DevicePageSceneItem();
        item.update(null, null, null, "EQUIPMENT", null, "Rack", "SR_RACK", SceneModelRole.RACK, 0, 0, 0, 0, 12, 12, 12);
        assertDoesNotThrow(() -> item.setGroupKey("10f47b83-62a1-4e8d-99cd-f2e755479c1f"));
        assertDoesNotThrow(() -> item.setGroupName("서버실 프레임"));
        assertThrows(IllegalArgumentException.class, () -> item.setGroupName("가".repeat(81)));
        assertThrows(IllegalArgumentException.class, () -> item.setGroupKey("bad-key"));
        assertDoesNotThrow(() -> item.setGroupKey(null));
        assertThrows(IllegalArgumentException.class, () -> item.setGroupName("그룹 없음"));
    }
    @Test void showroomModelsCanBeStoredAsEquipment() {
        for (String key : new String[]{"SR_RACK", "SR_DOUBLE_RACK", "SR_RDC", "SR_INROW", "SR_INROW_MOLD", "SR_WALL_COIL", "SR_DLC_RACK",
                "SR_INROW_RACK", "SR_OCP_RACK", "SR_RDC_RACK", "SR_ORV3_RACK", "SR_IMMERSION", "SR_HAC_RACK",
                "SR_DLC_CDU", "SR_DLC_SERVER", "SR_PDU_RACK", "SR_PDU_VERTICAL", "SR_GPU_SERVER"}) {
            DevicePageSceneItem item = new DevicePageSceneItem();
            assertDoesNotThrow(() -> item.update(null, null, null, "EQUIPMENT", null, key, key, roleFor(key), 0, 0, 0, 0, 4, 4), key);
        }
    }
    @Test void equipmentCanGrowWhileFloorDimensionsRemainBounded() {
        DevicePageSceneItem item = new DevicePageSceneItem();
        assertDoesNotThrow(() -> item.update(null, null, null, "EQUIPMENT", null, "Rack", "SR_RACK", SceneModelRole.RACK, 0, 0, 0, 0, 12, 15, 20));
        assertDoesNotThrow(() -> item.update(null, null, null, "EQUIPMENT", null, "Rack", "SR_RACK", SceneModelRole.RACK, 0, 0, 0, 0, 16, 4));
        assertDoesNotThrow(() -> item.update(null, null, null, "EQUIPMENT", null, "Rack", "SR_RACK", SceneModelRole.RACK, 0, 0, 0, 0, 32, 4));
        assertThrows(IllegalArgumentException.class, () -> item.update(null, null, null, "EQUIPMENT", null, "Rack", "SR_RACK", SceneModelRole.RACK, 0, 0, 0, 0, 33, 4));
        assertDoesNotThrow(() -> item.update(null, null, null, "EQUIPMENT", null, "Floor", "FLOOR", null, 0, 0, 0, 0, 5, 5));
        assertDoesNotThrow(() -> item.update(null, null, null, "EQUIPMENT", null, "Wall", "WALL_GLASS", null, 0, 0, 0, 0, 5, 5));
        assertThrows(IllegalArgumentException.class, () -> item.update(null, null, null, "EQUIPMENT", null, "Floor", "FLOOR", null, 0, 0, 0, 0, 6, 4));
    }
    @Test void bothEquipmentFramesCanGrowIndependentlyInHeightAndDepth() {
        DevicePageSceneItem item = new DevicePageSceneItem();
        for (String key : new String[]{"SR_INROW_MOLD", "SR_WALL_COIL"}) {
            assertDoesNotThrow(() -> item.update(null, null, null, "EQUIPMENT", null, key, key, roleFor(key), 0, 0, 0, 0, 32, 64, 96), key);
            assertThrows(IllegalArgumentException.class,
                    () -> item.update(null, null, null, "EQUIPMENT", null, key, key, roleFor(key), 0, 0, 0, 0, 32, 65, 96), key);
            assertThrows(IllegalArgumentException.class,
                    () -> item.update(null, null, null, "EQUIPMENT", null, key, key, roleFor(key), 0, 0, 0, 0, 32, 64, 97), key);
        }
    }
    @Test void wallAndCeilingAllowUpTo24Meters() {
        DevicePageSceneItem item = new DevicePageSceneItem();
        assertDoesNotThrow(() -> item.update(null, null, null, "EQUIPMENT", null, "Wall", "WALL_GLASS_CLEAR", null, 0, 0, 0, 0, 24, 4));
        assertDoesNotThrow(() -> item.update(null, null, null, "EQUIPMENT", null, "Ceiling", "CEILING", null, 0, 0, 0, 0, 24, 24));
        assertDoesNotThrow(() -> item.update(null, null, null, "EQUIPMENT", null, "Clear ceiling", "CEILING_CLEAR", null, 0, 0, 0, 0, 24, 24));
        assertThrows(IllegalArgumentException.class, () -> item.update(null, null, null, "EQUIPMENT", null, "Wall", "WALL_GLASS_CLEAR", null, 0, 0, 0, 0, 25, 4));
        assertThrows(IllegalArgumentException.class, () -> item.update(null, null, null, "EQUIPMENT", null, "Ceiling", "CEILING", null, 0, 0, 0, 0, 24, 25));
    }
}
