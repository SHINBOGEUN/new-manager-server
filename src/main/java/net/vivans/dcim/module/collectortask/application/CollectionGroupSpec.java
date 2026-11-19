package net.vivans.dcim.module.collectortask.application;

import java.util.List;

public record CollectionGroupSpec(
        Integer taskId,
        Integer groupId,
        Integer modelId,
        String protocol,
        String cronExpression,
        String community,
        int timeoutMs,
        int retries,
        int maxConcurrency,
        List<CollectionGroupOidSpec> oids,
        List<CollectionGroupTargetSpec> targets,
        List<String> skipped,
        Integer failureThreshold, Integer backoffMaxSeconds
) {
    public CollectionGroupSpec(Integer taskId,
        Integer groupId,
        Integer modelId,
        String protocol,
        String cronExpression,
        String community,
        int timeoutMs,
        int retries,
        int maxConcurrency,
        List<CollectionGroupOidSpec> oids,
        List<CollectionGroupTargetSpec> targets,
        List<String> skipped){this(taskId,groupId,modelId,protocol,cronExpression,community,timeoutMs,retries,maxConcurrency,oids,targets,skipped,null,null);}

}
