package net.vivans.dcim.module.lora.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.vivans.dcim.shared.persistence.BaseEntity;

import java.time.Instant;

/** Source 설정과 분리된, Sensor Data가 보고하는 연결/수신 상태 스냅샷. */
@Entity
@Table(name = "lora_mqtt_source_status")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoraMqttSourceStatus extends BaseEntity {

    @Id
    @Column(name = "source_id")
    private Integer sourceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LoraMqttSourceStatusType status;

    @Column(name = "last_connected_at")
    private Instant lastConnectedAt;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    @Column(name = "last_status_at")
    private Instant lastStatusAt;

    @Column(name = "message_count", nullable = false)
    private long messageCount;

    @Column(name = "error_count", nullable = false)
    private long errorCount;

    @Column(name = "reconnect_count", nullable = false)
    private long reconnectCount;

    @Column(name = "last_error", length = 2000)
    private String lastError;

    private LoraMqttSourceStatus(Integer sourceId) {
        this.sourceId = sourceId;
        this.status = LoraMqttSourceStatusType.NOT_SYNCED;
    }

    public static LoraMqttSourceStatus initial(Integer sourceId) {
        return new LoraMqttSourceStatus(sourceId);
    }

    public void report(LoraMqttSourceStatusType status, Instant lastConnectedAt, Instant lastMessageAt,
                       long messageCount, long errorCount, long reconnectCount, String lastError, Instant reportedAt) {
        this.status = status == null ? LoraMqttSourceStatusType.ERROR : status;
        this.lastConnectedAt = lastConnectedAt;
        this.lastMessageAt = lastMessageAt;
        this.messageCount = Math.max(0, messageCount);
        this.errorCount = Math.max(0, errorCount);
        this.reconnectCount = Math.max(0, reconnectCount);
        this.lastError = lastError == null || lastError.isBlank() ? null : lastError.substring(0, Math.min(lastError.length(), 2000));
        this.lastStatusAt = reportedAt == null ? Instant.now() : reportedAt;
    }
}
