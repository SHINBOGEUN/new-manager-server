package net.vivans.dcim.module.mqtt.application;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.mqtt.api.dto.MqttIdentityLookupResponse;
import net.vivans.dcim.module.mqtt.domain.model.MqttIdentityType;
import net.vivans.dcim.module.mqtt.domain.repository.DeviceMqttIdentityRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GpuAgentDiscoveryService {
    private final JdbcTemplate jdbc;
    private final GpuAgentRegistrationService registration;
    private final DeviceMqttIdentityRepository identities;

    @Transactional
    public void discover(String uuid, String hostname, JsonNode metadata) {
        String id = UUID.fromString(uuid).toString();
        var identity = identities.findByIdentityTypeAndNormalizedExternalId(MqttIdentityType.UUID, id);
        if (identity.isPresent() && (!identity.get().isEnabled() || !identity.get().getDevice().isEnabled())) return;
        String name = hostname == null || hostname.isBlank() ? "GPU server" : hostname.strip();
        name = name.substring(0, Math.min(name.length(), 200));
        String json = metadata == null ? null : metadata.toString();
        if (json != null && json.length() > 32768) throw new IllegalArgumentException("metadata too large");
        jdbc.update("""
            INSERT INTO gpu_agent_discovery(uuid, hostname, metadata_json, status, device_id)
            VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE
            hostname=VALUES(hostname), metadata_json=VALUES(metadata_json), last_seen=CURRENT_TIMESTAMP
            """, id, name, json, identity.isPresent() ? "APPROVED" : "PENDING",
                identity.map(value -> value.getDevice().getId()).orElse(null));
    }

    public List<Map<String, Object>> pending() {
        return jdbc.queryForList("SELECT uuid,hostname,metadata_json,first_seen,last_seen FROM gpu_agent_discovery WHERE status='PENDING' ORDER BY last_seen DESC LIMIT 500");
    }

    public List<Map<String, Object>> inventory(int deviceId) {
        return jdbc.queryForList("SELECT uuid,hostname,metadata_json,first_seen,last_seen FROM gpu_agent_discovery WHERE status='APPROVED' AND device_id=?", deviceId);
    }

    @Transactional
    public MqttIdentityLookupResponse approve(String uuid) {
        String id = UUID.fromString(uuid).toString();
        var rows = jdbc.queryForList("SELECT hostname FROM gpu_agent_discovery WHERE uuid=? AND status='PENDING' FOR UPDATE", id);
        if (rows.isEmpty()) throw new IllegalArgumentException("GPU agent is not pending");
        var result = registration.register(id, (String) rows.get(0).get("hostname"));
        jdbc.update("UPDATE gpu_agent_discovery SET status='APPROVED',device_id=? WHERE uuid=?", result.deviceId(), id);
        return result;
    }

    @Transactional
    public void reject(String uuid) {
        jdbc.update("UPDATE gpu_agent_discovery SET status='REJECTED' WHERE uuid=? AND status='PENDING'", UUID.fromString(uuid).toString());
    }
}

