package net.vivans.dcim.module.collectortask.api.dto;
import java.time.Instant;
import java.util.List;
import net.vivans.dcim.module.collectortask.application.CollectionReadiness;
public record DeviceCollectionSettingResponse(Integer deviceId,Integer protocolTypeId,String protocolCode,String protocolName,
    boolean supported,boolean persisted,boolean enabled,String intervalCode,String intervalName,Long intervalSeconds,
    Integer expectedSilenceSeconds,String state,boolean ready,List<CollectionReadiness> readiness,
    Integer taskId,Integer groupId,String groupName,String cronExpression,boolean inSync,Instant updatedDt,
        net.vivans.dcim.module.collectortask.application.CollectionRuntimeState collectionState
) {
    public DeviceCollectionSettingResponse(Integer deviceId,Integer protocolTypeId,String protocolCode,String protocolName,
    boolean supported,boolean persisted,boolean enabled,String intervalCode,String intervalName,Long intervalSeconds,
    Integer expectedSilenceSeconds,String state,boolean ready,List<CollectionReadiness> readiness,
    Integer taskId,Integer groupId,String groupName,String cronExpression,boolean inSync,Instant updatedDt){this(deviceId,protocolTypeId,protocolCode,protocolName,supported,persisted,enabled,intervalCode,intervalName,intervalSeconds,expectedSilenceSeconds,state,ready,readiness,taskId,groupId,groupName,cronExpression,inSync,updatedDt,null);}

    public DeviceCollectionSettingResponse withCollectionState(net.vivans.dcim.module.collectortask.application.CollectionRuntimeState value){return new DeviceCollectionSettingResponse(deviceId,protocolTypeId,protocolCode,protocolName,supported,persisted,enabled,intervalCode,intervalName,intervalSeconds,expectedSilenceSeconds,state,ready,readiness,taskId,groupId,groupName,cronExpression,inSync,updatedDt,value);}
}
