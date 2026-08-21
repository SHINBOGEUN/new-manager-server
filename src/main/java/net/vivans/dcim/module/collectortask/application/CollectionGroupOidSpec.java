package net.vivans.dcim.module.collectortask.application;

public record CollectionGroupOidSpec(
        String name,
        String template,
        boolean requiresInstance
) {
}
