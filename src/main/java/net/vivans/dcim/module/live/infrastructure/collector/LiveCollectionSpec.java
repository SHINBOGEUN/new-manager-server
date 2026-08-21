package net.vivans.dcim.module.live.infrastructure.collector;

import java.util.List;

public record LiveCollectionSpec(
        int intervalMs,
        String protocol,
        String community,
        int timeoutMs,
        int retries,
        int maxConcurrency,
        List<LiveCollectionTargetSpec> targets
) {
}
