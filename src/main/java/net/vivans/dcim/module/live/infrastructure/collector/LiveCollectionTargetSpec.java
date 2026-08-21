package net.vivans.dcim.module.live.infrastructure.collector;

import java.util.List;

public record LiveCollectionTargetSpec(
        Integer deviceId,
        String deviceName,
        String host,
        int port,
        Integer instanceId,
        List<LiveCollectionPointSpec> points
) {
}
