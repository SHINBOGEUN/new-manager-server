package net.vivans.dcim.module.collectortask.application;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.vivans.dcim.module.collectortask.api.dto.DeviceCollectionSettingRequest;
import net.vivans.dcim.module.collectortask.domain.model.*;
import net.vivans.dcim.module.collectortask.domain.repository.*;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import net.vivans.dcim.module.device.domain.repository.DeviceModbusReadingRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/** 잠금은 새 트랜잭션 바깥에서 잡고 커밋 뒤 푼다. 여러 장비의 쓰기는 묶지 않는다. */
@Service @RequiredArgsConstructor @Slf4j
public class CollectionReconciler {
    private final PlatformTransactionManager transactionManager;
    private final DeviceRepository devices;
    private final DeviceCollectionSettingRepository settings;
    private final CollectionTaskRepository tasks;
    private final DeviceModbusReadingRepository readings;
    private final CollectionIntervalResolver intervals;
    private final SystemSettingService system;
    private final CollectionScriptSyncService legacy;
    private final CollectionGroupSpecService specs;
    private final CollectorSyncService collector;
    private final EntityManager entityManager;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private final ConcurrentHashMap<Key,ReentrantLock> locks=new ConcurrentHashMap<>();
    private final ThreadLocal<TransactionStatus> currentTransaction=new ThreadLocal<>();
    public record Key(Integer modelId,Integer protocolId) implements Comparable<Key> {
        public int compareTo(Key other){int n=modelId.compareTo(other.modelId);return n!=0?n:protocolId.compareTo(other.protocolId);}
    }
    private static class Pending { final Map<String,Runnable> operations=new LinkedHashMap<>(); }
    private <T> T tx(Supplier<T> body) {
        TransactionTemplate t=new TransactionTemplate(transactionManager);t.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return t.execute(status->{
            TransactionStatus previous=currentTransaction.get();currentTransaction.set(status);
            try{return body.get();}finally{if(previous==null)currentTransaction.remove();else currentTransaction.set(previous);}
        });
    }
    public <T> T read(Supplier<T> body){return tx(body);}
    private void afterCommit(String key,Runnable work) {
        if(!TransactionSynchronizationManager.isActualTransactionActive() || !TransactionSynchronizationManager.isSynchronizationActive()){work.run();return;}
        Pending pending=(Pending)TransactionSynchronizationManager.getResource(this);
        if(pending==null){
            pending=new Pending();TransactionSynchronizationManager.bindResource(this,pending);Pending current=pending;
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){
                public void afterCommit(){
                    TransactionSynchronizationManager.unbindResourceIfPossible(CollectionReconciler.this);
                    for(Runnable operation:current.operations.values()) try{operation.run();}catch(RuntimeException e){log.warn("[COLLECTION_RECONCILE_ERROR] cause={}",e.getClass().getSimpleName());}
                }
                public void afterCompletion(int status){TransactionSynchronizationManager.unbindResourceIfPossible(CollectionReconciler.this);}
            });
        }
        pending.operations.put(key,work);
    }
    private <T> T locked(Collection<Key> keys,Supplier<T> body) {
        List<ReentrantLock> held=new ArrayList<>();
        try {
            for(Key key:keys.stream().distinct().sorted().toList()) {
                ReentrantLock lock=locks.computeIfAbsent(key,ignored->new ReentrantLock());
                if(!lock.tryLock(30,TimeUnit.SECONDS))throw new IllegalStateException("수집 설정 반영 대기 시간이 초과되었습니다.");held.add(lock);
            }
            for(int attempt=0;;attempt++)try{return tx(body);}catch(DataIntegrityViolationException e){if(attempt>=1)throw e;}
        }catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException("수집 설정 반영이 중단되었습니다.");}
        finally{Collections.reverse(held);held.forEach(ReentrantLock::unlock);}
    }
    private Device device(Integer id){return devices.findById(id).orElseThrow(()->new EntityNotFoundException("장비를 찾을 수 없습니다: "+id));}
    private List<Key> deviceKeys(Integer id){return tx(()->{
        Device d=device(id);Set<Key> keys=new TreeSet<>();
        d.getDeviceModel().getProtocols().forEach(p->keys.add(new Key(d.getDeviceModel().getId(),p.getProtocolType().getId())));
        settings.findAllByDeviceIdOrderByIdAsc(id).forEach(s->keys.add(new Key(d.getDeviceModel().getId(),s.getProtocolType().getId())));
        return List.copyOf(keys);
    });}
    private List<Key> modelKeys(Integer modelId){return tx(()->tasks.findAllByModelId(modelId).stream().map(t->new Key(modelId,t.getScriptType().getId())).toList());}
    public void reconcileDevice(Integer id){afterCommit("device:"+id,()->{
        if(system.autoEnabled()){
            boolean persisted=tx(()->!settings.findAllByDeviceIdOrderByIdAsc(id).isEmpty());
            if(persisted){refresh(applyExisting(id));return;}
        }
        Integer modelId=tx(()->device(id).getDeviceModel().getId());reconcileModelNow(modelId);
    });}
    public void reconcileModel(Integer modelId){if(modelId!=null)afterCommit("model:"+modelId,()->reconcileModelNow(modelId));}
    private void reconcileModelNow(Integer modelId){
        for(Key key:modelKeys(modelId))try{
            if(!system.autoEnabled())locked(List.of(key),()->{tasks.findByModelIdAndScriptTypeId(key.modelId(),key.protocolId()).ifPresent(legacy::regenerateTask);return null;});
            else refreshOne(key);
        }catch(RuntimeException e){log.warn("[COLLECTION_RECONCILE_ERROR] modelId={} protocol={} cause={}",key.modelId(),key.protocolId(),e.getClass().getSimpleName());}
    }
    public void reconcileMappedTarget(Integer id){if(id!=null)afterCommit("mapped:"+id,()->{
        Set<Integer> models=tx(()->{Set<Integer> ids=new LinkedHashSet<>();readings.findAllByTargetDeviceIdOrderByIdAsc(id).forEach(r->ids.add(r.getEndpointModbus().getEndpoint().getDevice().getDeviceModel().getId()));return ids;});
        models.forEach(this::reconcileModelNow);
    });}
    public void removeDevice(Integer id,Integer modelId){if(id==null || modelId==null)return;afterCommit("remove:"+id+":"+modelId,()->{
        List<Key> keys=modelKeys(modelId);Set<Key> changed=locked(keys,()->{Set<Key> result=new LinkedHashSet<>();for(CollectionTask task:tasks.findAllByModelId(modelId)){
            for(CollectionTaskGroup group:task.getGroups())if(group.containsDevice(id)){group.removeDevice(id);result.add(new Key(modelId,task.getScriptType().getId()));}
            // 삭제된 장비는 FK cascade로 이미 빠졌을 수 있으므로 spec도 재생성한다.
            result.add(new Key(modelId,task.getScriptType().getId()));tasks.saveAndFlush(task);
        }return result;});refresh(changed);
    });}
    /** 등록 경로의 기존 프로토콜 선택과 OFF 동작을 보존한다. */
    public void registerDevice(Device device){registerDevice(device,null);}
    public void registerDevice(Device device,String protocol){Integer id=device.getId();afterCommit("register:"+id,()->{
        if(system.autoEnabled()){
            Set<Key> changed=locked(deviceKeys(id),()->{
                Device d=device(id);for(var p:d.getDeviceModel().getProtocols()){
                    if(protocol!=null && !protocol.equalsIgnoreCase(p.getProtocolType().getCode()))continue;
                    String code=p.getProtocolType().getCode();if(!supported(code))continue;
                    if(settings.findByDeviceIdAndProtocolTypeId(id,p.getProtocolType().getId()).isEmpty())saveSetting(d,p.getProtocolType(),new DeviceCollectionSettingRequest(true,null,null));
                }return applyDevice(d);
            });refresh(changed);
        }else{
            List<Key> keys=deviceKeys(id);locked(keys,()->{Device d=device(id);if(protocol==null)legacy.assignDeviceAndRegenerate(d);else legacy.assignDeviceAndRegenerate(d,protocol);return null;});
        }
    });}
    public void configureRegistration(Integer id,Integer protocol,DeviceCollectionSettingRequest request){
        afterCommit("setting:"+id+":"+protocol,()->refresh(saveAndApply(id,protocol,request)));
    }
    public Set<Key> saveAndApply(Integer id,Integer protocol,DeviceCollectionSettingRequest request){return saveManyAndApply(id,List.of(protocol),request);}
    public Set<Key> saveManyAndApply(Integer id,List<Integer> protocols,DeviceCollectionSettingRequest request){
        return locked(deviceKeys(id),()->{
            Device d=device(id);
            for(Integer protocol:protocols){
                CommonCode p=d.getDeviceModel().getProtocols().stream().map(x->x.getProtocolType()).filter(x->x.getId().equals(protocol)).findFirst().orElseThrow(()->new IllegalArgumentException("장비 모델이 지원하지 않는 프로토콜입니다."));
                saveSetting(d,p,request);
            }return applyDevice(d);
        });
    }
    private void saveSetting(Device device,CommonCode protocol,DeviceCollectionSettingRequest request){
        String code=protocol.getCode();if(!supported(code))throw new IllegalArgumentException("지원하지 않는 수집 프로토콜입니다.");
        DeviceCollectionSetting row=settings.findByDeviceIdAndProtocolTypeId(device.getId(),protocol.getId()).orElseGet(()->new DeviceCollectionSetting(device,protocol));
        boolean enabled=request.enabled()!=null?request.enabled():row.getId()!=null?row.isEnabled():
            tasks.findByModelIdAndScriptTypeId(device.getDeviceModel().getId(),protocol.getId())
                .map(t->t.isActive() && t.getGroups().stream().anyMatch(g->g.isActive() && g.containsDevice(device.getId()))).orElse(false);
        CommonCode interval=row.getIntervalCode();Integer silence=request.expectedSilenceSeconds()==null?row.getExpectedSilenceSeconds():request.expectedSilenceSeconds();
        if("mqtt".equalsIgnoreCase(code)){
            if(request.intervalCode()!=null)throw new IllegalArgumentException("MQTT에는 조회 주기를 설정할 수 없습니다.");
            if(silence!=null && (silence<10 || silence>86400))throw new IllegalArgumentException("무수신 시간은 10~86400초로 입력하세요.");interval=null;
        }else{
            if(request.expectedSilenceSeconds()!=null)throw new IllegalArgumentException("무수신 시간은 MQTT에서만 설정합니다.");
            if(request.intervalCode()!=null)interval=intervals.code(request.intervalCode());else if(interval==null)interval=intervals.defaultCode();silence=null;
        }
        row.update(enabled,interval,silence);settings.saveAndFlush(row);
    }
    private Set<Key> applyExisting(Integer id){return locked(deviceKeys(id),()->applyDevice(device(id)));}
    private Set<Key> applyDevice(Device d){
        Set<Key> changed=new LinkedHashSet<>();
        for(DeviceCollectionSetting row:settings.findAllByDeviceIdOrderByIdAsc(d.getId())){
            CommonCode protocol=row.getProtocolType();String code=protocol.getCode();
            if(!CollectionGroupSpecService.isCollectorProtocol(code) || d.getDeviceModel().getProtocols().stream().noneMatch(p->p.getProtocolType().getId().equals(protocol.getId())))continue;
            CollectionTask task=tasks.findByModelIdAndScriptTypeId(d.getDeviceModel().getId(),protocol.getId()).orElseGet(()->tasks.saveAndFlush(CollectionTask.create(d.getDeviceModel().getName()+" "+code,d.getDeviceModel(),protocol,true)));
            CollectionTaskGroup target=null;
            if(row.isEnabled()){
                var interval=intervals.resolve(row.getIntervalCode());
                target=task.getGroups().stream().filter(g->g.getCronExpression().equals(interval.cron()) || CollectionIntervalResolver.equivalent(g.getCronExpression(),interval.cron()))
                    .sorted(Comparator.comparing(CollectionTaskGroup::isActive).reversed().thenComparing(CollectionTaskGroup::getId,Comparator.nullsLast(Integer::compareTo))).findFirst().orElse(null);
                if(target==null){target=CollectionTaskGroup.create(task,interval.name(),interval.cron(),true);entityManager.persist(target);entityManager.flush();}
            }
            for(CollectionTaskGroup group:task.getGroups())if(group!=target)group.removeDevice(d.getId());
            if(target!=null && !target.containsDevice(d.getId()))target.addDevice(d);
            tasks.saveAndFlush(task);changed.add(new Key(d.getDeviceModel().getId(),protocol.getId()));
        }return changed;
    }
    public static boolean supported(String code){return CollectionGroupSpecService.isCollectorProtocol(code) || "mqtt".equalsIgnoreCase(code);}
    /** 키별 재생성. 이 호출자는 트랜잭션 밖이어야 한다. */
    public boolean refresh(Collection<Key> keys){boolean ok=true;for(Key key:keys)try{refreshOne(key);}catch(RuntimeException e){ok=false;log.warn("[COLLECTION_RECONCILE_ERROR] modelId={} protocol={} cause={}",key.modelId(),key.protocolId(),e.getClass().getSimpleName());}return ok;}
    private void refreshOne(Key key){locked(List.of(key),()->{
        CollectionTask task=tasks.findByModelIdAndScriptTypeId(key.modelId(),key.protocolId()).orElse(null);if(task==null)return null;
        if(!CollectionGroupSpecService.isCollectorProtocol(task.getScriptType().getCode()))return null;
        record Generated(CollectionTaskGroup group,CollectionGroupPlan plan,String json) {}
        List<Generated> generated=new ArrayList<>();
        // 모든 그룹 판정을 먼저 끝낸다. 중간 DB 오류가 나면 기존 Collector spec은 건드리지 않는다.
        for(CollectionTaskGroup group:task.getGroups()){
            var plan=specs.generate(group);String json;
            try{json=objectMapper.writeValueAsString(plan.payload());}catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new IllegalStateException("수집 설정을 생성하지 못했습니다.");}
            if(!Objects.equals(json,group.getGeneratedSpec()))generated.add(new Generated(group,plan,json));
        }
        // T-044가 장비별로 잡은 저장소 예외도 참여 트랜잭션을 rollback-only로 만들 수 있다.
        // 이 경우 Collector에 외부 변경을 보내기 전에 중단한다(R-080).
        if(currentTransaction.get().isRollbackOnly())throw new IllegalStateException("수집 준비 판정의 DB 오류로 반영을 보류했습니다.");
        generated.forEach(g->g.group().updateGeneratedSpec(g.json()));tasks.saveAndFlush(task);
        for(Generated g:generated)collector.syncManagedGroup(g.group(),!g.plan().pointsBySourceDeviceId().isEmpty());
        return null;
    });}
    public void reconcileAll(){if(!system.autoEnabled())return;
        List<Integer> ids=tx(settings::deviceIds);Set<Key> changed=new LinkedHashSet<>();int failed=0;
        for(Integer id:ids)try{changed.addAll(applyExisting(id));}catch(RuntimeException e){failed++;log.warn("[COLLECTION_RECONCILE_ERROR] deviceId={} cause={}",id,e.getClass().getSimpleName());}
        refresh(changed);log.info("[COLLECTION_RECONCILE] devices={} changed={} failed={}",ids.size(),changed.size(),failed);
    }
}
