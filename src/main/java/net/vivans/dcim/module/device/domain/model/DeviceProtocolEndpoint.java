package net.vivans.dcim.module.device.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.shared.persistence.BaseEntity;

@Entity
@Table(
        name = "device_protocol_endpoint",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_device_protocol_endpoint_device_protocol",
                        columnNames = {"device_id", "protocol_type_id"}
                ),
                @UniqueConstraint(
                        name = "uk_device_protocol_endpoint_host_port",
                        columnNames = {"host", "port"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeviceProtocolEndpoint extends BaseEntity {

    private static final String PROTOCOL_TYPE_GROUP_KEY = "PROTOCOL_TYPE";
    private static final int MIN_PORT = 1;
    private static final int MAX_PORT = 65535;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "protocol_type_id", nullable = false)
    private CommonCode protocolType;

    @Column(nullable = false)
    private String host;

    @Column(nullable = false)
    private int port;

    @Column(nullable = false)
    private boolean enabled;

    private DeviceProtocolEndpoint(
            Device device,
            CommonCode protocolType,
            String host,
            int port,
            boolean enabled
    ) {
        validateDevice(device);
        validateProtocolType(protocolType);
        validateHost(host);
        validatePort(port);
        this.device = device;
        this.protocolType = protocolType;
        this.host = host;
        this.port = port;
        this.enabled = enabled;
    }

    public static DeviceProtocolEndpoint create(
            Device device,
            CommonCode protocolType,
            String host,
            int port,
            boolean enabled
    ) {
        return new DeviceProtocolEndpoint(device, protocolType, host, port, enabled);
    }

    public void update(CommonCode protocolType, String host, int port, boolean enabled) {
        validateProtocolType(protocolType);
        validateHost(host);
        validatePort(port);
        this.protocolType = protocolType;
        this.host = host;
        this.port = port;
        this.enabled = enabled;
    }

    private static void validateDevice(Device device) {
        if (device == null) {
            throw new IllegalArgumentException("device is required");
        }
    }

    private static void validateProtocolType(CommonCode protocolType) {
        if (protocolType == null) {
            throw new IllegalArgumentException("protocolType is required");
        }
        if (!PROTOCOL_TYPE_GROUP_KEY.equals(protocolType.getCodeGroup().getGroupKey())) {
            throw new IllegalArgumentException("protocolType must belong to PROTOCOL_TYPE group");
        }
    }

    private static void validateHost(String host) {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("host is required");
        }
    }

    private static void validatePort(int port) {
        if (port < MIN_PORT || port > MAX_PORT) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }
    }
}
