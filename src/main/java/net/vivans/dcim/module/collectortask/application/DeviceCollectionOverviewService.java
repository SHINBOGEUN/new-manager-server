package net.vivans.dcim.module.collectortask.application;

import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import net.vivans.dcim.module.collectortask.api.dto.DeviceCollectionSettingResponse;
import net.vivans.dcim.module.query.domain.*;
import net.vivans.dcim.module.query.config.CollectionStatusProperties;

/** React 전용 장비 중심 조회. 한 장비의 판정 실패는 그 장비만 확인 불가로 돌린다. */
@Service @RequiredArgsConstructor
public class DeviceCollectionOverviewService {
    private final DeviceRepository devices;
    private final DeviceCollectionSettingService settings;
    private final CollectionReconciler reconciler;
    private final CollectionRuntimeStateService runtime;
    private final CollectionEligibilityService eligibility;
    private final PointQuery points;
    private final CollectionStatusProperties properties;

    public record Value(Integer targetDeviceId,String targetDeviceName,String pointName,double value,String unit,Instant collectedAt) {}
    public record Protocol(DeviceCollectionSettingResponse setting,String state,String message,Instant latestCollectedAt,List<Value> latestValues) {}
    public record Row(Integer deviceId,String deviceName,Integer modelId,String modelName,String locationName,boolean deviceEnabled,List<Protocol> protocols,String state) {}
    public record Snapshot(Instant generatedAt,List<Row> devices) {}
    private record Meta(Integer id,String name,Integer modelId,String modelName,String location,boolean enabled) {}
    public Snapshot get() {
        List<Meta> all=reconciler.read(()->{
            List<Meta> result=new ArrayList<>();
            for(int page=0;;page++){
                var found=devices.findAll(null,null,null,null,null,null,PageRequest.of(page,200,org.springframework.data.domain.Sort.by("id")));
                found.forEach(d->result.add(new Meta(d.getId(),d.getName(),d.getDeviceModel().getId(),d.getDeviceModel().getName(),d.getLocationNode().getName(),d.isEnabled())));
                if(!found.hasNext())break;
            }return result;
        });
        List<Integer> ids=all.stream().map(Meta::id).toList();
        var executions=runtime.load(ids);
        var units=reconciler.read(()->eligibility.unitsByDevice(ids));
        List<String> pointNames=units.values().stream().flatMap(m->m.keySet().stream()).distinct().toList();
        Map<String,List<LastPoint>> values=new HashMap<>();Set<String> unavailable=new HashSet<>();
        for(String protocol:List.of("snmp","modbus","mqtt"))for(int start=0;start<ids.size();start+=200){
            var batch=ids.subList(start,Math.min(start+200,ids.size()));
            try{for(LastPoint point:pointNames.isEmpty()?List.<LastPoint>of():points.findLast(batch,pointNames,Duration.ofDays(7),protocol))values.computeIfAbsent(protocol+":"+point.deviceId(),ignored->new ArrayList<>()).add(point);}
            catch(RuntimeException failure){batch.forEach(id->unavailable.add(protocol+":"+id));}
        }
        List<Row> rows=new ArrayList<>();
        for(Meta meta:all){
            List<Protocol> protocols;boolean failed=false;
            try {
                var configs=reconciler.read(()->settings.getWithoutRuntime(meta.id()));
                protocols=new ArrayList<>();
                for(var config:configs){
                    var updated=config;
                    String jobId=reconciler.read(()->settings.collectorJobId(config));
                    if("READY".equals(config.state()) && jobId!=null)updated=config.withCollectionState(runtime.state(executions,jobId,meta.id()));
                    List<Value> latest=values.getOrDefault(config.protocolCode()+":"+meta.id(),List.of()).stream()
                        .map(v->new Value(meta.id(),meta.name(),v.pointName(),v.value(),units.getOrDefault(meta.id(),Map.of()).get(v.pointName()),v.time())).toList();
                    Instant latestAt=latest.stream().map(Value::collectedAt).filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null);
                    String state=config.state(),message=label(state);
                    if("READY".equals(state) || "WAITING_RECEIVE".equals(state)){
                        if(unavailable.contains(config.protocolCode()+":"+meta.id())){state="UNKNOWN";message="저장값 확인 불가";}
                        else if("mqtt".equals(config.protocolCode())){
                            var mqtt=reconciler.read(()->eligibility.mqttStateFromValues(meta.id(),config.expectedSilenceSeconds(),values.getOrDefault("mqtt:"+meta.id(),List.of())));
                            updated=updated.withDetails(updated.gateway(),mqtt);state=mqtt.state();message=mqtt.message();
                        }else if(updated.collectionState()!=null && !"OK".equals(updated.collectionState().state())){state=updated.collectionState().state();message=updated.collectionState().message();}
                        else if(latestAt==null){state="PENDING";message="시작 대기";}
                        else {
                            long interval=config.intervalSeconds()==null?properties.getDefaultIntervalSeconds():config.intervalSeconds();
                            if(config.gateway()!=null && config.gateway().resultDevice())interval=inheritedInterval(config.gateway());
                            long age=Math.max(0,Duration.between(latestAt,Instant.now()).getSeconds());
                            state=age>Math.max(properties.getMinFailureSeconds(),interval*properties.getFailureMultiplier())?"MISSING":age>Math.max(properties.getMinStaleSeconds(),interval*properties.getStaleMultiplier())?"STALE":"NORMAL";message=label(state);
                        }
                    }
                    protocols.add(new Protocol(updated,state,message,latestAt,latest));
                }
            }catch(RuntimeException failure){protocols=List.of();failed=true;}
            String worst=protocols.stream().map(Protocol::state).min(Comparator.comparingInt(DeviceCollectionOverviewService::priority)).orElse("WAITING_CONFIG");
            rows.add(new Row(meta.id(),meta.name(),meta.modelId(),meta.modelName(),meta.location(),meta.enabled(),List.copyOf(protocols),meta.enabled()?(failed?"UNKNOWN":worst):"DEVICE_DISABLED"));
        }
        return new Snapshot(Instant.now(),List.copyOf(rows));
    }
    private long inheritedInterval(CollectionEligibilityService.Gateway gateway){
        long seconds=0;
        for(var source:gateway.sources())if(source.enabled() && source.cronExpression()!=null)try{
            var cron=org.springframework.scheduling.support.CronExpression.parse(source.cronExpression());var a=cron.next(ZonedDateTime.now());var b=a==null?null:cron.next(a);
            if(b!=null)seconds=Math.max(seconds,Duration.between(a,b).getSeconds());
        }catch(RuntimeException ignored){}
        return seconds>0?seconds:properties.getDefaultIntervalSeconds();
    }
    private static int priority(String state){return switch(state){case "DEVICE_DISABLED"->0;case "OFF"->1;case "WAITING_CONFIG"->2;case "COMM_ERROR"->3;case "FAILING","UNKNOWN","SOURCE_STOPPED","STOPPED"->4;case "MISSING","STALE","PARTIAL"->5;case "PENDING","WAITING_RECEIVE"->6;default->7;};}
    private static String label(String state){return switch(state){case "DEVICE_DISABLED"->"장비 비활성";case "OFF"->"수집 꺼짐";case "WAITING_CONFIG"->"설정 대기";case "SOURCE_STOPPED"->"원본 수집 중지";case "STOPPED"->"수집 중지";case "MISSING"->"응답 없음";case "STALE"->"응답 지연";case "NORMAL"->"정상";default->"수집 상태 확인";};}
}
