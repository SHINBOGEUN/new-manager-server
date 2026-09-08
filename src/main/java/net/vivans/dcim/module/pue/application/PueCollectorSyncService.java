package net.vivans.dcim.module.pue.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.collectortask.infrastructure.collector.CollectorJobClient;
import net.vivans.dcim.module.device.domain.model.DeviceProtocolEndpoint;
import net.vivans.dcim.module.device.domain.model.DeviceSnmpInstance;
import net.vivans.dcim.module.device.domain.repository.DeviceProtocolEndpointRepository;
import net.vivans.dcim.module.device.domain.repository.DeviceSnmpInstanceRepository;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModelSnmpPoint;
import net.vivans.dcim.module.devicemodel.domain.repository.DeviceModelSnmpPointRepository;
import net.vivans.dcim.module.pue.domain.model.*;
import net.vivans.dcim.module.pue.domain.repository.PueDefinitionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;

@Slf4j
@Service @RequiredArgsConstructor
public class PueCollectorSyncService {
 private final CollectorJobClient client; private final ObjectMapper mapper; private final DeviceModelSnmpPointRepository points; private final DeviceProtocolEndpointRepository endpoints; private final DeviceSnmpInstanceRepository instances; private final PueDefinitionRepository definitions;
 public void sync(PueDefinition d){
  if(!client.isEnabled()){log.info("[COLLECTOR_SYNC_SKIP] type=PUE action=UPSERT definitionId={} reason=CLIENT_DISABLED",d.getId());return;}
  log.info("[COLLECTOR_SYNC_START] type=PUE action={} definitionId={} version={}",d.isCollectionEnabled()?"UPSERT":"DELETE",d.getId(),d.getConfigVersion());
  try{
   if(!d.isCollectionEnabled()){client.deletePue(d.getId());log.info("[COLLECTOR_SYNC_END] type=PUE action=DELETE definitionId={}",d.getId());return;}
   client.upsertPue(d.getId(),mapper.writeValueAsString(spec(d)));
   log.info("[COLLECTOR_SYNC_END] type=PUE action=UPSERT definitionId={} sourceCount={}",d.getId(),d.getSources().size());
  } catch(Exception e){
   log.warn("[COLLECTOR_SYNC_ERROR] type=PUE action={} definitionId={} exception={} message={}",d.isCollectionEnabled()?"UPSERT":"DELETE",d.getId(),e.getClass().getSimpleName(),e.getMessage());
   throw new IllegalStateException("PUE collector sync failed: "+e.getMessage(),e);
  }
 }
 public void remove(Integer id){
  if(!client.isEnabled()){log.info("[COLLECTOR_SYNC_SKIP] type=PUE action=DELETE definitionId={} reason=CLIENT_DISABLED",id);return;}
  log.info("[COLLECTOR_SYNC_START] type=PUE action=DELETE definitionId={}",id);
  try{client.deletePue(id);log.info("[COLLECTOR_SYNC_END] type=PUE action=DELETE definitionId={}",id);}
  catch(Exception e){log.warn("[COLLECTOR_SYNC_ERROR] type=PUE action=DELETE definitionId={} exception={} message={}",id,e.getClass().getSimpleName(),e.getMessage());throw new IllegalStateException("PUE collector delete failed: "+e.getMessage(),e);}
 }
 public void repushActiveDefinitions(){if(!client.isEnabled())return;for(PueDefinition definition:definitions.findAllByCollectionEnabled(true)){try{sync(definition);log.info("PUE collector job repushed: definitionId={}",definition.getId());}catch(Exception e){log.warn("PUE collector job repush failed: definitionId={}, reason={}",definition.getId(),e.getMessage());}}}
 private Spec spec(PueDefinition d){List<Source> result=new ArrayList<>();for(PueDefinitionSource s:d.getSources()){DeviceModelSnmpPoint p=points.findAllEnabledByDeviceModelIds(Set.of(s.getDevice().getDeviceModel().getId())).stream().filter(x->x.getName().equalsIgnoreCase(s.getPointName())).findFirst().orElseThrow(()->new IllegalArgumentException("PUE source must be enabled SNMP point: "+s.getPointName()));DeviceProtocolEndpoint e=endpoints.findAllByDeviceIdOrderByIdAsc(s.getDevice().getId()).stream().filter(x->x.isEnabled()&&"snmp".equalsIgnoreCase(x.getProtocolType().getCode())).findFirst().orElseThrow(()->new IllegalArgumentException("PUE source has no enabled SNMP endpoint: "+s.getDevice().getId()));Integer instance=instances.findByEndpointId(e.getId()).map(DeviceSnmpInstance::getInstanceId).orElse(null);String oid=p.resolveOid(instance);if(oid==null)throw new IllegalArgumentException("PUE source missing SNMP instance: "+s.getDevice().getId());result.add(new Source(s.getDevice().getId(),e.getHost(),e.getPort(),s.getPointName(),oid,p.getScale(),s.getRole().name()));}return new Spec(d.getId(),d.getConfigVersion(),d.getCalculationCron(),"public",2000,1,result);}
 record Spec(Integer pueDefinitionId,Integer configVersion,String cronExpression,String community,int timeoutMs,int retries,List<Source> sources){} record Source(Integer deviceId,String host,int port,String pointName,String oid,Double scale,String role){}
}
