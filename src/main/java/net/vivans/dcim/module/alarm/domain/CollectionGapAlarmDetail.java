package net.vivans.dcim.module.alarm.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.vivans.dcim.module.device.domain.model.Device;

@Entity
@Table(name = "collection_gap_alarm_event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CollectionGapAlarmDetail {
    @Id
    @Column(name = "event_id")
    private Long eventId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "event_id", nullable = false)
    private AlarmEvent event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(nullable = false, length = 20)
    private String protocol;

    public CollectionGapAlarmDetail(AlarmEvent event, Device device, String protocol) {
        this.event = event;
        this.device = device;
        this.protocol = protocol;
    }
}
