package net.vivans.dcim.module.device.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.vivans.dcim.bootstrap.ManagerServerApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static net.vivans.dcim.support.AuthTestSupport.bearerToken;
import static net.vivans.dcim.support.AuthTestSupport.loginAndGetAccessToken;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ManagerServerApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class DeviceProtocolEndpointControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createEndpoint_returnsCreatedEndpoint() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "endpoint-create-user", "password123");
        Integer snmpId = snmpProtocolTypeId(accessToken);
        Integer modelId = createDeviceModel(accessToken, "AP8959", "APC", snmpId);
        int deviceId = createDevice(accessToken, modelId, "PDU-좌");

        mockMvc.perform(post("/api/manager/devices/{deviceId}/endpoints", deviceId)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "protocolTypeId": %d,
                                  "host": "192.168.1.10",
                                  "port": 161,
                                  "enabled": true
                                }
                                """.formatted(snmpId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.deviceId").value(deviceId))
                .andExpect(jsonPath("$.data.protocolTypeId").value(snmpId))
                .andExpect(jsonPath("$.data.protocolCode").value("snmp"))
                .andExpect(jsonPath("$.data.protocolName").value("SNMP"))
                .andExpect(jsonPath("$.data.host").value("192.168.1.10"))
                .andExpect(jsonPath("$.data.port").value(161))
                .andExpect(jsonPath("$.data.enabled").value(true));
    }

    @Test
    void createEndpoint_withoutEnabled_defaultsToTrue() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "endpoint-create-default", "password123");
        Integer snmpId = snmpProtocolTypeId(accessToken);
        Integer modelId = createDeviceModel(accessToken, "LHT65N", "Dragino", snmpId);
        int deviceId = createDevice(accessToken, modelId, "센서-01");

        mockMvc.perform(post("/api/manager/devices/{deviceId}/endpoints", deviceId)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "protocolTypeId": %d,
                                  "host": "10.0.0.1",
                                  "port": 161
                                }
                                """.formatted(snmpId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true));
    }

    @Test
    void createEndpoint_whenDeviceNotFound_returnsNotFound() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "endpoint-create-device-nf", "password123");
        Integer snmpId = snmpProtocolTypeId(accessToken);

        mockMvc.perform(post("/api/manager/devices/{deviceId}/endpoints", 999999)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "protocolTypeId": %d,
                                  "host": "192.168.1.10",
                                  "port": 161
                                }
                                """.formatted(snmpId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Device not found: 999999"));
    }

    @Test
    void createEndpoint_whenProtocolNotSupportedByModel_returnsBadRequest() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "endpoint-create-unsupported", "password123");
        Integer snmpId = snmpProtocolTypeId(accessToken);
        Integer modbusId = findOrCreateCommonCode(
                accessToken,
                findOrCreateCodeGroup(accessToken, "PROTOCOL_TYPE", "Protocol Type"),
                "modbus",
                "Modbus",
                2
        );
        Integer modelId = createDeviceModel(accessToken, "AP8959-SNMP-ONLY", "APC", snmpId);
        int deviceId = createDevice(accessToken, modelId, "PDU-modbus-fail");

        mockMvc.perform(post("/api/manager/devices/{deviceId}/endpoints", deviceId)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "protocolTypeId": %d,
                                  "host": "192.168.1.10",
                                  "port": 502
                                }
                                """.formatted(modbusId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("protocol not supported by device model"));
    }

    @Test
    void createEndpoint_withDuplicateProtocol_returnsConflict() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "endpoint-create-dup", "password123");
        Integer snmpId = snmpProtocolTypeId(accessToken);
        Integer modelId = createDeviceModel(accessToken, "AP8959-DUP", "APC", snmpId);
        int deviceId = createDevice(accessToken, modelId, "PDU-dup");

        String body = """
                {
                  "protocolTypeId": %d,
                  "host": "192.168.1.10",
                  "port": 161
                }
                """.formatted(snmpId);

        mockMvc.perform(post("/api/manager/devices/{deviceId}/endpoints", deviceId)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/manager/devices/{deviceId}/endpoints", deviceId)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("endpoint already exists for this protocol"));
    }

    @Test
    void createEndpoint_whenHostPortAlreadyUsedByAnotherDevice_returnsConflict() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "endpoint-create-host-port-dup", "password123");
        Integer snmpId = snmpProtocolTypeId(accessToken);
        Integer modelId = createDeviceModel(accessToken, "AP8959-HOST-DUP", "APC", snmpId);
        int deviceA = createDevice(accessToken, modelId, "PDU-host-a");
        int deviceB = createDevice(accessToken, modelId, "PDU-host-b");
        createEndpoint(accessToken, deviceA, snmpId, "192.168.1.10", 161);

        mockMvc.perform(post("/api/manager/devices/{deviceId}/endpoints", deviceB)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "protocolTypeId": %d,
                                  "host": "192.168.1.10",
                                  "port": 161
                                }
                                """.formatted(snmpId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("endpoint already exists for this host and port"));
    }

    @Test
    void createEndpoint_withNonProtocolType_returnsBadRequest() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "endpoint-create-bad-type", "password123");
        Integer modelTypeGroupId = findOrCreateCodeGroup(accessToken, "MODEL_TYPE", "Model Type");
        Integer pduTypeId = findOrCreateCommonCode(accessToken, modelTypeGroupId, "PDU", "PDU", 1);
        Integer snmpId = snmpProtocolTypeId(accessToken);
        Integer modelId = createDeviceModel(accessToken, "AP8959-BAD-TYPE", "APC", snmpId);
        int deviceId = createDevice(accessToken, modelId, "PDU-bad-type");

        mockMvc.perform(post("/api/manager/devices/{deviceId}/endpoints", deviceId)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "protocolTypeId": %d,
                                  "host": "192.168.1.10",
                                  "port": 161
                                }
                                """.formatted(pduTypeId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("protocolType must belong to PROTOCOL_TYPE group"));
    }

    @Test
    void createEndpoint_withBlankHost_returnsBadRequest() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "endpoint-create-blank-host", "password123");
        Integer snmpId = snmpProtocolTypeId(accessToken);
        Integer modelId = createDeviceModel(accessToken, "AP8959-BLANK", "APC", snmpId);
        int deviceId = createDevice(accessToken, modelId, "PDU-blank");

        mockMvc.perform(post("/api/manager/devices/{deviceId}/endpoints", deviceId)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "protocolTypeId": %d,
                                  "host": " ",
                                  "port": 161
                                }
                                """.formatted(snmpId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid value for parameter 'host'"));
    }

    @Test
    void createEndpoint_withInvalidPort_returnsBadRequest() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "endpoint-create-bad-port", "password123");
        Integer snmpId = snmpProtocolTypeId(accessToken);
        Integer modelId = createDeviceModel(accessToken, "AP8959-PORT", "APC", snmpId);
        int deviceId = createDevice(accessToken, modelId, "PDU-port");

        mockMvc.perform(post("/api/manager/devices/{deviceId}/endpoints", deviceId)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "protocolTypeId": %d,
                                  "host": "192.168.1.10",
                                  "port": 0
                                }
                                """.formatted(snmpId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid value for parameter 'port'"));
    }

    @Test
    void updateEndpoint_returnsUpdatedEndpoint() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "endpoint-update-user", "password123");
        Integer snmpId = snmpProtocolTypeId(accessToken);
        Integer modelId = createDeviceModel(accessToken, "AP8959-UPD", "APC", snmpId);
        int deviceId = createDevice(accessToken, modelId, "PDU-upd");
        int endpointId = createEndpoint(accessToken, deviceId, snmpId, "192.168.1.10", 161);

        mockMvc.perform(put("/api/manager/devices/{deviceId}/endpoints/{endpointId}", deviceId, endpointId)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "protocolTypeId": %d,
                                  "host": "10.0.0.5",
                                  "port": 1161,
                                  "enabled": false
                                }
                                """.formatted(snmpId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(endpointId))
                .andExpect(jsonPath("$.data.deviceId").value(deviceId))
                .andExpect(jsonPath("$.data.protocolTypeId").value(snmpId))
                .andExpect(jsonPath("$.data.host").value("10.0.0.5"))
                .andExpect(jsonPath("$.data.port").value(1161))
                .andExpect(jsonPath("$.data.enabled").value(false));
    }

    @Test
    void updateEndpoint_whenEndpointNotFound_returnsNotFound() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "endpoint-update-nf", "password123");
        Integer snmpId = snmpProtocolTypeId(accessToken);
        Integer modelId = createDeviceModel(accessToken, "AP8959-UPD-NF", "APC", snmpId);
        int deviceId = createDevice(accessToken, modelId, "PDU-upd-nf");

        mockMvc.perform(put("/api/manager/devices/{deviceId}/endpoints/{endpointId}", deviceId, 999999)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "protocolTypeId": %d,
                                  "host": "10.0.0.5",
                                  "port": 161
                                }
                                """.formatted(snmpId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("DeviceProtocolEndpoint not found: 999999"));
    }

    @Test
    void updateEndpoint_whenDeviceNotFound_returnsNotFound() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "endpoint-update-device-nf", "password123");
        Integer snmpId = snmpProtocolTypeId(accessToken);

        mockMvc.perform(put("/api/manager/devices/{deviceId}/endpoints/{endpointId}", 999999, 1)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "protocolTypeId": %d,
                                  "host": "10.0.0.5",
                                  "port": 161
                                }
                                """.formatted(snmpId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Device not found: 999999"));
    }

    @Test
    void updateEndpoint_whenProtocolNotSupportedByModel_returnsBadRequest() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "endpoint-update-unsupported", "password123");
        Integer snmpId = snmpProtocolTypeId(accessToken);
        Integer modbusId = findOrCreateCommonCode(
                accessToken,
                findOrCreateCodeGroup(accessToken, "PROTOCOL_TYPE", "Protocol Type"),
                "modbus",
                "Modbus",
                2
        );
        Integer modelId = createDeviceModel(accessToken, "AP8959-UPD-UNSUP", "APC", snmpId);
        int deviceId = createDevice(accessToken, modelId, "PDU-upd-unsup");
        int endpointId = createEndpoint(accessToken, deviceId, snmpId, "192.168.1.10", 161);

        mockMvc.perform(put("/api/manager/devices/{deviceId}/endpoints/{endpointId}", deviceId, endpointId)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "protocolTypeId": %d,
                                  "host": "192.168.1.10",
                                  "port": 502
                                }
                                """.formatted(modbusId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("protocol not supported by device model"));
    }

    @Test
    void updateEndpoint_withDuplicateProtocol_returnsConflict() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "endpoint-update-dup", "password123");
        Integer protocolGroupId = findOrCreateCodeGroup(accessToken, "PROTOCOL_TYPE", "Protocol Type");
        Integer snmpId = findOrCreateCommonCode(accessToken, protocolGroupId, "snmp", "SNMP", 1);
        Integer modbusId = findOrCreateCommonCode(accessToken, protocolGroupId, "modbus", "Modbus", 2);
        Integer modelId = createDeviceModelWithProtocols(
                accessToken, "AP8959-UPD-DUP", "APC", snmpId, modbusId);
        int deviceId = createDevice(accessToken, modelId, "PDU-upd-dup");
        createEndpoint(accessToken, deviceId, snmpId, "192.168.1.10", 161);
        int modbusEndpointId = createEndpoint(accessToken, deviceId, modbusId, "192.168.1.10", 502);

        mockMvc.perform(put("/api/manager/devices/{deviceId}/endpoints/{endpointId}", deviceId, modbusEndpointId)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "protocolTypeId": %d,
                                  "host": "192.168.1.10",
                                  "port": 161
                                }
                                """.formatted(snmpId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("endpoint already exists for this protocol"));
    }

    @Test
    void updateEndpoint_whenHostPortAlreadyUsedByAnotherDevice_returnsConflict() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "endpoint-update-host-port-dup", "password123");
        Integer snmpId = snmpProtocolTypeId(accessToken);
        Integer modelId = createDeviceModel(accessToken, "AP8959-UPD-HOST-DUP", "APC", snmpId);
        int deviceA = createDevice(accessToken, modelId, "PDU-upd-host-a");
        int deviceB = createDevice(accessToken, modelId, "PDU-upd-host-b");
        createEndpoint(accessToken, deviceA, snmpId, "192.168.1.10", 161);
        int endpointB = createEndpoint(accessToken, deviceB, snmpId, "192.168.1.11", 161);

        mockMvc.perform(put("/api/manager/devices/{deviceId}/endpoints/{endpointId}", deviceB, endpointB)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "protocolTypeId": %d,
                                  "host": "192.168.1.10",
                                  "port": 161
                                }
                                """.formatted(snmpId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("endpoint already exists for this host and port"));
    }

    @Test
    void getEndpoints_returnsEndpointsOrderedById() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "endpoint-list-user", "password123");
        Integer protocolGroupId = findOrCreateCodeGroup(accessToken, "PROTOCOL_TYPE", "Protocol Type");
        Integer snmpId = findOrCreateCommonCode(accessToken, protocolGroupId, "snmp", "SNMP", 1);
        Integer modbusId = findOrCreateCommonCode(accessToken, protocolGroupId, "modbus", "Modbus", 2);
        Integer modelId = createDeviceModelWithProtocols(
                accessToken, "AP8959-LIST", "APC", snmpId, modbusId);
        int deviceId = createDevice(accessToken, modelId, "PDU-list");
        int snmpEndpointId = createEndpoint(accessToken, deviceId, snmpId, "192.168.1.10", 161);
        int modbusEndpointId = createEndpoint(accessToken, deviceId, modbusId, "192.168.1.10", 502);

        mockMvc.perform(get("/api/manager/devices/{deviceId}/endpoints", deviceId)
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].id").value(snmpEndpointId))
                .andExpect(jsonPath("$.data[0].protocolCode").value("snmp"))
                .andExpect(jsonPath("$.data[0].host").value("192.168.1.10"))
                .andExpect(jsonPath("$.data[0].port").value(161))
                .andExpect(jsonPath("$.data[1].id").value(modbusEndpointId))
                .andExpect(jsonPath("$.data[1].protocolCode").value("modbus"))
                .andExpect(jsonPath("$.data[1].port").value(502));
    }

    @Test
    void getEndpoints_whenEmpty_returnsEmptyList() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "endpoint-list-empty", "password123");
        Integer snmpId = snmpProtocolTypeId(accessToken);
        Integer modelId = createDeviceModel(accessToken, "AP8959-LIST-EMPTY", "APC", snmpId);
        int deviceId = createDevice(accessToken, modelId, "PDU-list-empty");

        mockMvc.perform(get("/api/manager/devices/{deviceId}/endpoints", deviceId)
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void getEndpoints_whenDeviceNotFound_returnsNotFound() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "endpoint-list-device-nf", "password123");

        mockMvc.perform(get("/api/manager/devices/{deviceId}/endpoints", 999999)
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Device not found: 999999"));
    }

    @Test
    void getEndpoint_returnsEndpoint() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "endpoint-get-user", "password123");
        Integer snmpId = snmpProtocolTypeId(accessToken);
        Integer modelId = createDeviceModel(accessToken, "AP8959-GET", "APC", snmpId);
        int deviceId = createDevice(accessToken, modelId, "PDU-get");
        int endpointId = createEndpoint(accessToken, deviceId, snmpId, "192.168.1.10", 161);

        mockMvc.perform(get("/api/manager/devices/{deviceId}/endpoints/{endpointId}", deviceId, endpointId)
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(endpointId))
                .andExpect(jsonPath("$.data.deviceId").value(deviceId))
                .andExpect(jsonPath("$.data.protocolTypeId").value(snmpId))
                .andExpect(jsonPath("$.data.protocolCode").value("snmp"))
                .andExpect(jsonPath("$.data.host").value("192.168.1.10"))
                .andExpect(jsonPath("$.data.port").value(161))
                .andExpect(jsonPath("$.data.enabled").value(true));
    }

    @Test
    void getEndpoint_whenNotFound_returnsNotFound() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "endpoint-get-nf", "password123");
        Integer snmpId = snmpProtocolTypeId(accessToken);
        Integer modelId = createDeviceModel(accessToken, "AP8959-GET-NF", "APC", snmpId);
        int deviceId = createDevice(accessToken, modelId, "PDU-get-nf");

        mockMvc.perform(get("/api/manager/devices/{deviceId}/endpoints/{endpointId}", deviceId, 999999)
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("DeviceProtocolEndpoint not found: 999999"));
    }

    @Test
    void deleteEndpoint_deletesAndReturnsId() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "endpoint-delete-user", "password123");
        Integer snmpId = snmpProtocolTypeId(accessToken);
        Integer modelId = createDeviceModel(accessToken, "AP8959-DEL", "APC", snmpId);
        int deviceId = createDevice(accessToken, modelId, "PDU-del");
        int endpointId = createEndpoint(accessToken, deviceId, snmpId, "192.168.1.10", 161);

        mockMvc.perform(delete("/api/manager/devices/{deviceId}/endpoints/{endpointId}", deviceId, endpointId)
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(endpointId));

        mockMvc.perform(get("/api/manager/devices/{deviceId}/endpoints/{endpointId}", deviceId, endpointId)
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("DeviceProtocolEndpoint not found: " + endpointId));
    }

    @Test
    void deleteEndpoint_whenNotFound_returnsNotFound() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "endpoint-delete-nf", "password123");
        Integer snmpId = snmpProtocolTypeId(accessToken);
        Integer modelId = createDeviceModel(accessToken, "AP8959-DEL-NF", "APC", snmpId);
        int deviceId = createDevice(accessToken, modelId, "PDU-del-nf");

        mockMvc.perform(delete("/api/manager/devices/{deviceId}/endpoints/{endpointId}", deviceId, 999999)
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("DeviceProtocolEndpoint not found: 999999"));
    }

    @Test
    void deleteEndpoint_whenDeviceNotFound_returnsNotFound() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "endpoint-delete-device-nf", "password123");

        mockMvc.perform(delete("/api/manager/devices/{deviceId}/endpoints/{endpointId}", 999999, 1)
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Device not found: 999999"));
    }

    private Integer snmpProtocolTypeId(String accessToken) throws Exception {
        Integer groupId = findOrCreateCodeGroup(accessToken, "PROTOCOL_TYPE", "Protocol Type");
        return findOrCreateCommonCode(accessToken, groupId, "snmp", "SNMP", 1);
    }

    private int createEndpoint(
            String accessToken,
            int deviceId,
            Integer protocolTypeId,
            String host,
            int port
    ) throws Exception {
        String response = mockMvc.perform(post("/api/manager/devices/{deviceId}/endpoints", deviceId)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "protocolTypeId": %d,
                                  "host": "%s",
                                  "port": %d
                                }
                                """.formatted(protocolTypeId, host, port)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).path("data").path("id").asInt();
    }

    private int createDevice(String accessToken, Integer modelId, String name) throws Exception {
        String locationCode = createRootLocation(accessToken, "Rack-" + name);

        String response = mockMvc.perform(post("/api/manager/devices")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "modelId": %d,
                                  "locationNodeCode": "%s",
                                  "name": "%s"
                                }
                                """.formatted(modelId, locationCode, name)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).path("data").path("id").asInt();
    }

    private Integer createDeviceModel(
            String accessToken,
            String name,
            String manufacturer,
            Integer protocolTypeId
    ) throws Exception {
        Integer modelTypeGroupId = findOrCreateCodeGroup(accessToken, "MODEL_TYPE", "Model Type");
        Integer deviceTypeId = findOrCreateCommonCode(accessToken, modelTypeGroupId, "PDU", "PDU", 1);

        String response = mockMvc.perform(post("/api/manager/device-models")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "manufacturer": "%s",
                                  "deviceTypeId": %d,
                                  "protocols": [
                                    { "protocolTypeId": %d }
                                  ]
                                }
                                """.formatted(name, manufacturer, deviceTypeId, protocolTypeId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).path("data").path("id").asInt();
    }

    private Integer createDeviceModelWithProtocols(
            String accessToken,
            String name,
            String manufacturer,
            Integer protocolTypeId1,
            Integer protocolTypeId2
    ) throws Exception {
        Integer modelTypeGroupId = findOrCreateCodeGroup(accessToken, "MODEL_TYPE", "Model Type");
        Integer deviceTypeId = findOrCreateCommonCode(accessToken, modelTypeGroupId, "PDU", "PDU", 1);

        String response = mockMvc.perform(post("/api/manager/device-models")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "manufacturer": "%s",
                                  "deviceTypeId": %d,
                                  "protocols": [
                                    { "protocolTypeId": %d },
                                    { "protocolTypeId": %d }
                                  ]
                                }
                                """.formatted(name, manufacturer, deviceTypeId, protocolTypeId1, protocolTypeId2)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).path("data").path("id").asInt();
    }

    private String createRootLocation(String accessToken, String name) throws Exception {
        Integer groupId = findOrCreateCodeGroup(accessToken, "LOCATION_TYPE", "Location Type");
        Integer rackTypeId = findOrCreateCommonCode(accessToken, groupId, "RACK", "랙", 3);
        return createLocationNode(accessToken, rackTypeId, name);
    }

    private String createLocationNode(String accessToken, Integer locationTypeId, String name) throws Exception {
        String response = mockMvc.perform(post("/api/manager/location-node")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"parentCode": null, "locationTypeId": %d, "name": "%s"}
                                """.formatted(locationTypeId, name)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).path("data").path("code").asText();
    }

    private Integer findOrCreateCodeGroup(String accessToken, String groupKey, String groupName) throws Exception {
        String listResponse = mockMvc.perform(get("/api/manager/code-groups")
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        for (JsonNode node : objectMapper.readTree(listResponse).path("data")) {
            if (groupKey.equals(node.path("groupKey").asText())) {
                return node.path("id").asInt();
            }
        }

        String response = mockMvc.perform(post("/api/manager/code-groups")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"groupKey": "%s", "groupName": "%s"}
                                """.formatted(groupKey, groupName)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).path("data").path("id").asInt();
    }

    private Integer findOrCreateCommonCode(
            String accessToken,
            Integer groupId,
            String code,
            String name,
            Integer sortOrder
    ) throws Exception {
        String listResponse = mockMvc.perform(get("/api/manager/common-codes")
                        .param("codeGroupId", String.valueOf(groupId))
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        for (JsonNode node : objectMapper.readTree(listResponse).path("data")) {
            if (code.equals(node.path("code").asText())) {
                return node.path("id").asInt();
            }
        }

        String response = mockMvc.perform(post("/api/manager/common-codes")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"groupId": %d, "code": "%s", "name": "%s", "sortOrder": %d}
                                """.formatted(groupId, code, name, sortOrder)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).path("data").path("id").asInt();
    }
}
