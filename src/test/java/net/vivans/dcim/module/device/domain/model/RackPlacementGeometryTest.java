package net.vivans.dcim.module.device.domain.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RackPlacementGeometryTest {
    @Test void rackUModelCanChangeStartAndHeight() {
        var model = RackModelPlacement.create("0000000001", "MODEL", DeviceMountType.RACK_U, null, 2, 4, null, null, null);
        model.setUPlacement(7, 5);
        assertEquals(7, model.getUPosition()); assertEquals(5, model.getUHeight()); assertEquals(11, model.lastU());
        assertThrows(IllegalArgumentException.class, () -> model.setUPlacement(1, 0));
        assertEquals(5, model.getUHeight());
    }
    @Test void sideLengthIsBoundedAndCanBeStoredForModelOnly() {
        var model = RackModelPlacement.create("0000000001", "MODEL", DeviceMountType.RACK_SIDE, DeviceRackSide.LEFT, null, null, .03, .5, .9);
        model.setVisualLengthRatio(.75); assertEquals(.75, model.getVisualLengthRatio());
        assertThrows(IllegalArgumentException.class, () -> model.setVisualLengthRatio(1.01));
        assertThrows(IllegalArgumentException.class, () -> model.setVisualLengthRatio(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> model.setUPlacement(1, 4));
        assertEquals(.75, model.getVisualLengthRatio());
    }
}
