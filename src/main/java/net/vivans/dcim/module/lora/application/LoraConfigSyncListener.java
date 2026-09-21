package net.vivans.dcim.module.lora.application;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.lora.infrastructure.sensordata.LoraMqttSourceSyncClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class LoraConfigSyncListener {
    private final LoraMqttSourceSyncClient syncClient;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChanged(LoraConfigChangedEvent event) {
        syncClient.requestConfigRefresh(event.reason());
    }
}
