package net.vivans.dcim.module.live.infrastructure.collector;

public record LiveCollectionPointSpec(
        String name,
        String template,
        boolean requiresInstance,
        String unit
) {
}
