package net.vivans.dcim.module.lora.application;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.lora.infrastructure.sensordata.LoraMqttSourceSyncClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class LoraMqttSourceSyncListener {
    private final LoraMqttSourceSyncClient syncClient;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChanged(LoraMqttSourceChangedEvent event) {
        syncClient.requestRefresh(event.reason());
    }
}
