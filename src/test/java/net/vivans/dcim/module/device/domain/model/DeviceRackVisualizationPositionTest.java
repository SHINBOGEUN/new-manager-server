package net.vivans.dcim.module.device.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DeviceRackVisualizationPositionTest {
    @Test
    void coordinatesMustBeCompleteAndWithinRackBounds() {
        DeviceRackPlacement placement = new DeviceRackPlacement();
        placement.setVisualizationPosition(0.0, 0.85, 1.0);
        assertEquals(0.85, placement.getPositionY());
        assertThrows(IllegalArgumentException.class, () -> placement.setVisualizationPosition(0.0, null, 1.0));
        assertThrows(IllegalArgumentException.class, () -> placement.setVisualizationPosition(-0.01, 0.0, 1.0));
        assertThrows(IllegalArgumentException.class, () -> placement.setVisualizationPosition(0.0, 1.01, 1.0));
        assertThrows(IllegalArgumentException.class, () -> placement.setVisualizationPosition(Double.NaN, 0.0, 1.0));
        placement.setVisualizationPosition(null, null, null);
        assertNull(placement.getPositionX());
    }
}
