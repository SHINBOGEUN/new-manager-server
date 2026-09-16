package net.vivans.dcim.module.device.domain.model;

public enum DeviceMountType {
    RACK_U,
    RACK_SIDE,
    RACK_REAR,
    FLOOR,
    WALL;

    public boolean requiresRack() {
        return this == RACK_U || this == RACK_SIDE || this == RACK_REAR;
    }

    public boolean usesRackU() {
        return this == RACK_U;
    }
}
