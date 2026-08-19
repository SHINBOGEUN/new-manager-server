package net.vivans.dcim.module.collectortask.application;

public record CollectionGroupTargetSpec(
        Integer deviceId,
        String host,
        int port,
        Integer instanceId
) {
}
