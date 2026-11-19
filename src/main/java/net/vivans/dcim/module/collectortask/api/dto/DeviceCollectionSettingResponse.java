package net.vivans.dcim.module.collectortask.api.dto;
import java.time.Instant;
import java.util.List;
import net.vivans.dcim.module.collectortask.application.CollectionReadiness;
public record DeviceCollectionSettingResponse(Integer deviceId,Integer protocolTypeId,String protocolCode,String protocolName,
    boolean supported,boolean persisted,boolean enabled,String intervalCode,String intervalName,Long intervalSeconds,
    Integer expectedSilenceSeconds,String state,boolean ready,List<CollectionReadiness> readiness,
    Integer taskId,Integer groupId,String groupName,String cronExpression,boolean inSync,Instant updatedDt) {}
