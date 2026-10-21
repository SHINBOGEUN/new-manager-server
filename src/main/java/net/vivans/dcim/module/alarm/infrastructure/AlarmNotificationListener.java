package net.vivans.dcim.module.alarm.infrastructure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.vivans.dcim.module.alarm.application.AlarmCreated;
import net.vivans.dcim.module.alarm.application.AlarmQueryService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
@RequiredArgsConstructor
public class AlarmNotificationListener {
    private final AlarmQueryService queries;
    private final AlarmWebSocketHandler sockets;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCreated(AlarmCreated created) {
        try {
            var event = queries.forNotification(created.eventId());
            if (event != null) sockets.publish(event);
        } catch (RuntimeException exception) {
            log.warn("Alarm socket notification failed eventId={}: {}", created.eventId(), exception.getMessage());
        }
    }
}
