package net.vivans.dcim.module.mqtt.application;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.mqtt.infrastructure.sensordata.MqttSourceSyncClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class MqttSourceSyncListener {
    private final MqttSourceSyncClient syncClient;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChanged(MqttSourceChangedEvent event) {
        syncClient.requestRefresh(event.reason());
    }
}
