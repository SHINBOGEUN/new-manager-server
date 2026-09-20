package net.vivans.dcim.module.lora.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.shared.persistence.BaseEntity;

import java.time.Instant;

/**
 * LoRa 수집 중 발생한 미등록 식별자·미매핑 필드·변환 실패 이력.
 * Sensor Data는 자체 RDB가 없어 Manager API로 이 표에 적재한다.
 * raw_payload는 호출 측에서 이미 길이를 제한해 보낸다고 가정하되, 방어적으로 한 번 더 자른다.
 */
@Entity
@Table(name = "lora_ingest_error_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoraIngestErrorLog extends BaseEntity {

    public static final int RAW_PAYLOAD_MAX_LENGTH = 4000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private Device device;

    @Column(name = "external_id")
    private String externalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "id_type", length = 20)
    private LoraIdType idType;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(name = "raw_payload", length = 4000)
    private String rawPayload;

    @Column(nullable = false)
    private boolean resolved;

    private LoraIngestErrorLog(
            Instant receivedAt, Device device, String externalId, LoraIdType idType,
            String reason, String rawPayload
    ) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason is required");
        }
        this.receivedAt = receivedAt == null ? Instant.now() : receivedAt;
        this.device = device;
        this.externalId = externalId;
        this.idType = idType;
        this.reason = reason;
        this.rawPayload = truncate(rawPayload);
        this.resolved = false;
    }

    public static LoraIngestErrorLog create(
            Instant receivedAt, Device device, String externalId, LoraIdType idType,
            String reason, String rawPayload
    ) {
        return new LoraIngestErrorLog(receivedAt, device, externalId, idType, reason, rawPayload);
    }

    public void markResolved(boolean resolved) {
        this.resolved = resolved;
    }

    private static String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > RAW_PAYLOAD_MAX_LENGTH ? value.substring(0, RAW_PAYLOAD_MAX_LENGTH) : value;
    }
}
