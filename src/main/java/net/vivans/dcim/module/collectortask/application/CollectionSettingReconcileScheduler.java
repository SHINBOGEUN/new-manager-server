package net.vivans.dcim.module.collectortask.application;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import net.vivans.dcim.module.collectortask.domain.repository.DeviceCollectionSettingRepository;
@Component @RequiredArgsConstructor @Slf4j
public class CollectionSettingReconcileScheduler {
    private final CollectionReconciler reconciler;
    private final SystemSettingService system;
    private final DeviceCollectionSettingRepository settings;
    private final DeviceCollectionSettingService service;
    @Scheduled(fixedDelayString="${collection.reconcile.fixed-delay-ms:300000}",initialDelayString="${collection.reconcile.fixed-delay-ms:300000}")
    public void reconcile(){reconciler.reconcileAll();}
    @EventListener(ApplicationReadyEvent.class) public void verify(){
        if(!system.autoEnabled())return;long mismatched=0;
        for(Integer id:settings.deviceIds())try{mismatched+=service.get(id).stream().filter(s->!s.inSync()).count();}catch(RuntimeException e){mismatched++;}
        log.info("[COLLECTION_RECONCILE_VERIFY] mismatched={}",mismatched);
    }
}
