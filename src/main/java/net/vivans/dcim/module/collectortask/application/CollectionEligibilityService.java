package net.vivans.dcim.module.collectortask.application;

import java.util.*;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.domain.model.*;
import net.vivans.dcim.module.device.domain.repository.*;
import net.vivans.dcim.module.device.infrastructure.persistence.DeviceModbusBitFieldRepository;
import net.vivans.dcim.module.collectortask.domain.repository.*;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.mqtt.domain.model.*;
import net.vivans.dcim.module.mqtt.domain.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 저장 허가와 원본 관계는 캐시하지 않는다. 설정 없는 기존 장비는 기존 수집을 유지한다. */
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class CollectionEligibilityService {
    private final DeviceRepository devices;
    private final DeviceCollectionSettingRepository settings;
    private final DeviceProtocolEndpointRepository endpoints;
    private final DeviceModbusReadingRepository readings;
    private final DeviceModbusBitFieldRepository bitFields;
    private final CollectionTaskRepository tasks;
    private final CollectionIntervalResolver intervals;
    private final DeviceMqttIdentityRepository identities;
    private final DeviceModelMqttPointRepository mqttPoints;
    private final MqttSourceRepository sources;
    private final net.vivans.dcim.module.devicemodel.application.DeviceModelPointCatalog catalog;
    private final DeviceMonitoringPolicyRepository policies;
    private final net.vivans.dcim.module.query.domain.PointQuery pointQuery;
    private final net.vivans.dcim.module.query.config.CollectionStatusProperties statusProperties;
    public record MqttState(String state,String message,java.time.Instant lastReceivedAt,long expectedSilenceSeconds,boolean onChange,List<net.vivans.dcim.module.query.domain.LastPoint> latestValues) {}
    public MqttState mqttState(Integer id,Integer silence) {

        List<net.vivans.dcim.module.query.domain.LastPoint> values;
        try {var names=devices.findById(id).map(d->mqttPoints.findAllByDeviceModelIdOrderByIdAsc(d.getDeviceModel().getId()).stream().filter(DeviceModelMqttPoint::isEnabled).map(DeviceModelMqttPoint::getPointName).distinct().toList()).orElse(List.of());values=names.isEmpty()?List.of():pointQuery.findLast(List.of(id),names,java.time.Duration.ofDays(7),"mqtt");}
        catch(RuntimeException failure){return new MqttState("UNKNOWN","최근 수신 확인 불가",null,silence==null?statusProperties.getDefaultIntervalSeconds():silence,false,List.of());}
        return mqttStateFromValues(id,silence,values);
    }
    public MqttState mqttStateFromValues(Integer id,Integer silence,List<net.vivans.dcim.module.query.domain.LastPoint> values){
        boolean onChange=policies.findByDeviceId(id).map(p->"ON_CHANGE".equals(p.getMonitoringMode().getCode())).orElse(false);
        long seconds=silence==null?Math.max(10,statusProperties.getDefaultIntervalSeconds()):silence;
        java.time.Instant latest=values.stream().map(net.vivans.dcim.module.query.domain.LastPoint::time).filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null);
        String state=latest==null?"WAITING_RECEIVE":onChange?"ON_CHANGE":latest.plusSeconds(seconds).isBefore(java.time.Instant.now())?"STALE":"OK";
        String message=switch(state){case "WAITING_RECEIVE"->"수신 대기";case "ON_CHANGE"->"변경 시 수신";case "STALE"->"수신 지연";default->"정상 수신";};
        return new MqttState(state,message,latest,seconds,onChange,values);
    }
    public record Source(Integer deviceId,String deviceName,boolean enabled,String cronExpression,String intervalName,Long intervalSeconds) {
        public Source(Integer deviceId,String deviceName,boolean enabled,String cronExpression){this(deviceId,deviceName,enabled,cronExpression,null,null);}
    }
    public record Gateway(boolean resultDevice,List<Source> sources,int affectedDeviceCount,List<Integer> affectedDeviceIds) {}
    public record WriteRequest(Integer deviceId,String protocol,Integer sourceDeviceId,List<String> pointNames,String identityType,String externalId) {}
    public record WritePermission(boolean allowed,List<String> pointNames) {}
    public boolean enabled(Integer id,String protocol) {
        return id!=null && devices.findById(id).filter(Device::isEnabled).isPresent() && collectionOn(id,protocol);
    }
    public boolean collectionOn(Integer id,String protocol) {
        return settings.findAllByDeviceIdOrderByIdAsc(id).stream()
            .filter(s->s.getProtocolType().getCode().equalsIgnoreCase(protocol)).allMatch(s->s.isEnabled());
    }
    public List<DeviceModbusReading> incoming(Integer id) {
        return readings.findAllByTargetDeviceIdOrderByIdAsc(id).stream()
            .filter(x->!x.getEndpointModbus().getEndpoint().getDevice().getId().equals(id)).toList();
    }
    public boolean resultOnly(Integer id,String protocol) {
        return "modbus".equalsIgnoreCase(protocol) && !incoming(id).isEmpty()
            && endpoints.findAllByDeviceIdOrderByIdAsc(id).stream().noneMatch(e->e.getProtocolType().getCode().equalsIgnoreCase(protocol));
    }
    public Optional<CommonCode> derivedProtocol(Integer id,Integer protocolId) {
        return incoming(id).stream().map(x->x.getEndpointModbus().getEndpoint().getProtocolType())
            .filter(p->protocolId==null || p.getId().equals(protocolId)).findFirst();
    }
    public Gateway gateway(Integer id) {
        Map<Integer,Source> origin=new LinkedHashMap<>();
        for(var row:incoming(id)) {
            Device source=row.getEndpointModbus().getEndpoint().getDevice();
            var task=tasks.findByModelIdAndScriptTypeId(source.getDeviceModel().getId(),row.getEndpointModbus().getEndpoint().getProtocolType().getId()).orElse(null);
            var group=task==null?null:task.getGroups().stream().filter(g->g.containsDevice(source.getId())).sorted(Comparator.comparing(g->!g.isActive())).findFirst().orElse(null);
            boolean active=row.isEnabled() && row.getPoint().isEnabled() && enabled(source.getId(),"modbus") && row.getEndpointModbus().getEndpoint().isEnabled()
                && task!=null && task.isActive() && group!=null && group.isActive();
            String cron=group==null?null:group.getCronExpression();
            var interval=cron==null?null:intervals.intervals().stream().filter(i->CollectionIntervalResolver.equivalent(i.cron(),cron)).findFirst().orElse(null);
            Source previous=origin.get(source.getId());
            origin.put(source.getId(),new Source(source.getId(),source.getName(),active || previous!=null && previous.enabled(),cron,interval==null?null:interval.name(),interval==null?null:interval.seconds()));
        }
        Set<Integer> affected=new LinkedHashSet<>();
        for(var endpoint:endpoints.findAllByDeviceIdOrderByIdAsc(id))
            if("modbus".equalsIgnoreCase(endpoint.getProtocolType().getCode()))
                for(var row:readings.findAllByEndpointIdOrderByIdAsc(endpoint.getId()))
                    if(row.isEnabled() && row.getPoint().isEnabled() && !id.equals(row.getTargetDevice().getId()))affected.add(row.getTargetDevice().getId());
        return new Gateway(resultOnly(id,"modbus"),List.copyOf(origin.values()),affected.size(),List.copyOf(affected));
    }
    public Map<Integer,Map<String,String>> unitsByDevice(List<Integer> ids) {
        Map<Integer,Device> selected=new LinkedHashMap<>();ids.forEach(id->devices.findById(id).ifPresent(d->selected.put(id,d)));
        var models=catalog.unitsByModelId(selected.values().stream().map(d->d.getDeviceModel().getId()).distinct().toList());
        Map<Integer,Map<String,String>> mqtt=new HashMap<>(),result=new HashMap<>();
        for(var d:selected.values()){
            int model=d.getDeviceModel().getId();Map<String,String> units=new LinkedHashMap<>(models.getOrDefault(model,Map.of()));
            units.putAll(mqtt.computeIfAbsent(model,key->{Map<String,String> values=new LinkedHashMap<>();mqttPoints.findAllByDeviceModelIdOrderByIdAsc(key).stream().filter(DeviceModelMqttPoint::isEnabled).forEach(p->values.put(p.getPointName(),p.getUnit()));return values;}));
            result.put(d.getId(),units);
        }
        if(!ids.isEmpty())for(var reading:readings.findAllByTargetDeviceIds(ids))if(reading.isEnabled() && reading.getPoint().isEnabled()){
            var units=result.get(reading.getTargetDevice().getId());if(units==null)continue;
            units.put(reading.getPointName(),reading.getPoint().getUnit());bitFields.findAllByReading_IdOrderByIdAsc(reading.getId()).forEach(f->units.putIfAbsent(f.getPointName(),null));
        }
        return result;
    }
    public List<CollectionReadiness> mqttReadiness(Device device) {
        List<CollectionReadiness> result=new ArrayList<>();
        var ids=identities.findAllByDeviceId(device.getId()).stream().filter(DeviceMqttIdentity::isEnabled).toList();
        if(ids.isEmpty())result.add(new CollectionReadiness(CollectionReadiness.Code.NO_IDENTITY,"MQTT 식별값","/ops-console.html?page=devices"));
        boolean source=sources.findAllOrderByIdAsc().stream().anyMatch(s->s.isEnabled() && (ids.isEmpty() || ids.stream().anyMatch(i->i.getIdentityType().isLora()==(s.getSourceType()==MqttSourceType.CHIRPSTACK_LORA))));
        if(!source)result.add(new CollectionReadiness(CollectionReadiness.Code.NO_SOURCE,"활성 MQTT 수신 소스","/ops-console.html?page=lora-mqtt-sources"));
        if(mqttPoints.findAllByDeviceModelIdOrderByIdAsc(device.getDeviceModel().getId()).stream().noneMatch(DeviceModelMqttPoint::isEnabled))
            result.add(new CollectionReadiness(CollectionReadiness.Code.NO_MAPPING,"MQTT 측정 항목 매핑","/ops-console.html?page=models"));
        return List.copyOf(result);
    }
    public WritePermission authorize(WriteRequest request) {
        if(request.deviceId()==null || request.protocol()==null || request.pointNames()==null || request.pointNames().size()>2000)
            throw new IllegalArgumentException("유효한 장비·프로토콜·측정 항목이 필요합니다.");
        String protocol=request.protocol().toLowerCase(Locale.ROOT);
        if(!Set.of("mqtt","snmp","modbus").contains(protocol) || !enabled(request.deviceId(),protocol))return new WritePermission(false,List.of());
        if("mqtt".equals(protocol)) {
            if(request.identityType()==null || request.externalId()==null)return new WritePermission(false,List.of());
            MqttIdentityType type=MqttIdentityType.valueOf(request.identityType());
            boolean match=identities.findByIdentityTypeAndNormalizedExternalIdAndEnabledTrue(type,MqttExternalIdNormalizer.normalize(type,request.externalId()))
                .filter(i->i.getDevice().getId().equals(request.deviceId())).isPresent();
            return new WritePermission(match,match?List.copyOf(request.pointNames()):List.of());
        }
        var mappings=incoming(request.deviceId());
        if(request.sourceDeviceId()!=null && !enabled(request.sourceDeviceId(),protocol))return new WritePermission(false,List.of());
        // 직접 수집에는 원본=저장 대상. 매핑 결과는 원본·포인트별로 독립 허가한다.
        if("modbus".equals(protocol) && request.sourceDeviceId()!=null && !request.deviceId().equals(request.sourceDeviceId()) && mappings.isEmpty())return new WritePermission(false,List.of());
        if(!"modbus".equals(protocol) || mappings.isEmpty() || request.deviceId().equals(request.sourceDeviceId()))
            return new WritePermission(true,List.copyOf(request.pointNames()));
        Set<String> allowed=new HashSet<>();
        for(var row:mappings) {
            if(!row.isEnabled() || !row.getPoint().isEnabled())continue;
            var endpoint=row.getEndpointModbus().getEndpoint();
            if(!endpoint.isEnabled() || !enabled(endpoint.getDevice().getId(),protocol))continue;
            if(request.sourceDeviceId()!=null && !endpoint.getDevice().getId().equals(request.sourceDeviceId()))continue;
            allowed.add(row.getPointName());
            bitFields.findAllByReading_IdOrderByIdAsc(row.getId()).forEach(f->allowed.add(f.getPointName()));
        }
        List<String> points=request.pointNames().stream().filter(allowed::contains).toList();
        return new WritePermission(!points.isEmpty(),points);
    }
}
