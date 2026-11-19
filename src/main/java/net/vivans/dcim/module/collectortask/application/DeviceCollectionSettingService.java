package net.vivans.dcim.module.collectortask.application;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import jakarta.persistence.EntityNotFoundException;
import net.vivans.dcim.module.collectortask.api.dto.*;
import net.vivans.dcim.module.collectortask.domain.model.*;
import net.vivans.dcim.module.collectortask.domain.repository.*;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;

@Service @RequiredArgsConstructor
public class DeviceCollectionSettingService {
    private final DeviceRepository devices;
    private final DeviceCollectionSettingRepository rows;
    private final CollectionTaskRepository tasks;
    private final CollectionIntervalResolver intervals;
    private final SystemSettingService system;
    private final CollectionReconciler reconciler;
    private final CollectionGroupSpecService specs;
    private final CollectionRuntimeStateService runtimeStates;
    public record Options(List<CollectionIntervalResolver.Interval> intervals,String defaultIntervalCode,boolean autoEnabled,
        Integer failureThreshold, Integer backoffMaxSeconds
) {
    public Options(List<CollectionIntervalResolver.Interval> intervals,String defaultIntervalCode,boolean autoEnabled){this(intervals,defaultIntervalCode,autoEnabled,null,null);}
}
    public record SystemRequest(String defaultIntervalCode,Boolean autoEnabled,
        Integer failureThreshold, Integer backoffMaxSeconds
) {
    public SystemRequest(String defaultIntervalCode,Boolean autoEnabled){this(defaultIntervalCode,autoEnabled,null,null);}
}
    public record BulkRequest(List<Integer> deviceIds,Integer protocolTypeId,Boolean enabled,String intervalCode) {}
    public record BulkResult(Integer deviceId,Integer protocolTypeId,boolean ok,String message) {}
    public record BulkResponse(List<BulkResult> results,long succeeded,long failed) {}
    @Transactional(readOnly=true) public Options options(){return new Options(intervals.intervals(),intervals.defaultInterval().code(),system.autoEnabled(),system.failureThreshold(),system.backoffMaxSeconds());}
    @Transactional public Options updateSystem(SystemRequest request){
        if(request.defaultIntervalCode()!=null){intervals.code(request.defaultIntervalCode());system.put(SystemSettingService.DEFAULT_INTERVAL,request.defaultIntervalCode());}
        if(request.autoEnabled()!=null)system.put(SystemSettingService.AUTO,request.autoEnabled().toString());
        boolean changed=false;
        if(request.failureThreshold()!=null){
            if(request.failureThreshold()<1 || request.failureThreshold()>20)throw new IllegalArgumentException("연속 실패 기준은 1~20회입니다.");
            changed|=request.failureThreshold()!=system.failureThreshold();system.put("collection.failure.threshold",request.failureThreshold().toString());
        }
        if(request.backoffMaxSeconds()!=null){
            if(request.backoffMaxSeconds()<0 || request.backoffMaxSeconds()>86400)throw new IllegalArgumentException("백오프 상한은 0~86400초입니다.");
            changed|=request.backoffMaxSeconds()!=system.backoffMaxSeconds();system.put("collection.backoff.max-seconds",request.backoffMaxSeconds().toString());
        }
        if(changed)reconciler.reconcileActiveGroupsAfterCommit();return options();
    }
    @Transactional(readOnly=true) public List<DeviceCollectionSettingResponse> get(Integer id){return getRows(id,true);}
    @Transactional(readOnly=true) public List<DeviceCollectionSettingResponse> getWithoutRuntime(Integer id){return getRows(id,false);}
    private List<DeviceCollectionSettingResponse> getRows(Integer id,boolean includeRuntime){
        Device d=devices.findById(id).orElseThrow(()->new EntityNotFoundException("장비를 찾을 수 없습니다: "+id));
        Map<Integer,DeviceCollectionSetting> saved=new LinkedHashMap<>();rows.findAllByDeviceIdOrderByIdAsc(id).forEach(s->saved.put(s.getProtocolType().getId(),s));
        List<CommonCode> protocols=new ArrayList<>();d.getDeviceModel().getProtocols().forEach(p->{if(CollectionReconciler.supported(p.getProtocolType().getCode()))protocols.add(p.getProtocolType());});
        saved.values().forEach(s->{if(protocols.stream().noneMatch(p->p.getId().equals(s.getProtocolType().getId())))protocols.add(s.getProtocolType());});
        List<DeviceCollectionSettingResponse> result=new ArrayList<>();for(CommonCode p:protocols)result.add(view(d,p,saved.get(p.getId())));
        if(includeRuntime && result.stream().anyMatch(s->"READY".equals(s.state()) && s.groupId()!=null)){
            var runtime=runtimeStates.load(List.of(id));
            result=result.stream().map(s->"READY".equals(s.state()) && s.groupId()!=null && taskJobId(s)!=null?s.withCollectionState(runtimeStates.state(runtime,taskJobId(s),id)):s).toList();
        }
        return result;
    }
    private String taskJobId(DeviceCollectionSettingResponse setting){
        return setting.taskId()==null?null:tasks.findById(setting.taskId()).stream().flatMap(t->t.getGroups().stream()).filter(g->g.getId().equals(setting.groupId())).map(CollectionTaskGroup::getCollectorJobId).filter(Objects::nonNull).findFirst().orElse(null);
    }
    private DeviceCollectionSettingResponse view(Device d,CommonCode p,DeviceCollectionSetting saved){
        boolean supported=d.getDeviceModel().getProtocols().stream().anyMatch(x->x.getProtocolType().getId().equals(p.getId()));
        boolean mqtt="mqtt".equalsIgnoreCase(p.getCode());
        CollectionTask task=tasks.findByModelIdAndScriptTypeId(d.getDeviceModel().getId(),p.getId()).orElse(null);
        List<CollectionTaskGroup> memberships=task==null?List.of():task.getGroups().stream().filter(g->g.containsDevice(d.getId())).sorted(Comparator.comparing(CollectionTaskGroup::isActive).reversed().thenComparing(CollectionTaskGroup::getId)).toList();
        CollectionTaskGroup group=memberships.stream().findFirst().orElse(null);
        boolean enabled=saved!=null?saved.isEnabled():group!=null && task.isActive() && group.isActive();
        CollectionIntervalResolver.Interval interval=saved!=null && saved.getIntervalCode()!=null?intervals.resolve(saved.getIntervalCode()):null;
        if(interval==null && !mqtt && group!=null)interval=intervals.intervals().stream().filter(i->CollectionIntervalResolver.equivalent(i.cron(),group.getCronExpression())).findFirst().orElse(null);
        CollectionGroupPlan plan=null;
        if(supported && !mqtt){
            if(group!=null)plan=specs.generate(group);
            else {
                CollectionTask probe=CollectionTask.create(d.getDeviceModel().getName()+" "+p.getCode(),d.getDeviceModel(),p,true);
                CollectionTaskGroup check=CollectionTaskGroup.create(probe,"준비 확인",interval==null?intervals.defaultInterval().cron():interval.cron(),true);check.addDevice(d);plan=specs.generate(check);
            }
        }
        boolean ready=plan!=null && plan.includesSource(d.getId());List<CollectionReadiness> readiness=new ArrayList<>();
        if(!supported)readiness.add(new CollectionReadiness(CollectionReadiness.Code.NO_PROTOCOL,"장비 모델의 지원 프로토콜","/ops-console.html?page=models"));
        else if(plan!=null){
            plan.reasons().stream().filter(reason->d.getId().equals(reason.deviceId()) || !ready && reason.deviceId()==null).map(CollectionGroupPlan.SkippedReason::readiness).distinct().forEach(readiness::add);
        }
        String state=mqtt?null:!d.isEnabled()?"DEVICE_DISABLED":saved!=null && !enabled?"OFF":group!=null && (!task.isActive() || !group.isActive())?"STOPPED":!ready?"WAITING_CONFIG":"READY";
        boolean inSync=saved==null || mqtt || (!enabled?memberships.isEmpty():memberships.size()==1 && interval!=null && CollectionIntervalResolver.equivalent(group.getCronExpression(),interval.cron()));
        return new DeviceCollectionSettingResponse(d.getId(),p.getId(),p.getCode().toLowerCase(Locale.ROOT),p.getName(),supported,saved!=null,enabled,
            interval==null?null:interval.code(),interval==null?null:interval.name(),interval==null?null:interval.seconds(),saved==null?null:saved.getExpectedSilenceSeconds(),state,ready,List.copyOf(readiness),
            task==null?null:task.getId(),group==null?null:group.getId(),group==null?null:group.getName(),group==null?null:group.getCronExpression(),inSync,saved==null?null:saved.getUpdatedDt());
    }
    public DeviceCollectionSettingResponse save(Integer id,Integer protocol,DeviceCollectionSettingRequest request){
        checkPermission(request.intervalCode(),request.expectedSilenceSeconds());
        if(request.enabled()==null && request.intervalCode()==null && request.expectedSilenceSeconds()==null)throw new IllegalArgumentException("변경할 수집 설정이 없습니다.");
        Set<CollectionReconciler.Key> keys=reconciler.saveAndApply(id,protocol,request);reconciler.refresh(keys);
        return reconciler.read(()->getWithoutRuntime(id).stream().filter(s->s.protocolTypeId().equals(protocol)).findFirst().orElseThrow());
    }
    public BulkResponse bulk(BulkRequest request){
        checkPermission(request.intervalCode(),null);
        if(request.deviceIds()==null || request.deviceIds().isEmpty() || request.deviceIds().size()>500 || request.deviceIds().stream().anyMatch(Objects::isNull))throw new IllegalArgumentException("장비는 1~500대 선택하세요.");
        if(request.enabled()==null && request.intervalCode()==null)throw new IllegalArgumentException("변경할 수집 설정이 없습니다.");
        List<BulkResult> results=new ArrayList<>();Set<CollectionReconciler.Key> changed=new LinkedHashSet<>();
        for(Integer id:new LinkedHashSet<>(request.deviceIds())){
            List<Integer> protocols;
            try{protocols=request.protocolTypeId()==null?reconciler.read(()->getWithoutRuntime(id).stream().filter(s->s.supported() && CollectionGroupSpecService.isCollectorProtocol(s.protocolCode())).map(DeviceCollectionSettingResponse::protocolTypeId).toList()):List.of(request.protocolTypeId());}
            catch(RuntimeException e){results.add(new BulkResult(id,request.protocolTypeId(),false,"장비 수집 설정을 확인할 수 없습니다."));continue;}
            if(protocols.isEmpty()){results.add(new BulkResult(id,null,false,"지원하는 조회 프로토콜이 없습니다."));continue;}
            try{changed.addAll(reconciler.saveManyAndApply(id,protocols,new DeviceCollectionSettingRequest(request.enabled(),request.intervalCode(),null)));for(Integer protocol:protocols)results.add(new BulkResult(id,protocol,true,"저장됨"));}
            catch(RuntimeException e){for(Integer protocol:protocols)results.add(new BulkResult(id,protocol,false,"프로토콜·주기·장비 설정을 확인하세요."));}
        }
        boolean refreshed=reconciler.refresh(changed);
        if(!refreshed)results=results.stream().map(x->x.ok()?new BulkResult(x.deviceId(),x.protocolTypeId(),true,"저장됨 · 반영 지연, 자동 재시도 예정"):x).toList();
        long ok=results.stream().filter(BulkResult::ok).count();return new BulkResponse(List.copyOf(results),ok,results.size()-ok);
    }
    private static void checkPermission(String interval,Integer silence){
        boolean admin=SecurityContextHolder.getContext().getAuthentication()!=null && SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().anyMatch(a->"ROLE_ADMIN".equals(a.getAuthority()));
        if(!admin && (interval!=null || silence!=null))throw new AccessDeniedException("주기·무수신 시간은 관리자만 변경할 수 있습니다.");
    }
}
