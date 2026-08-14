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
class DeviceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createDevice_returnsCreatedDevice() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "device-create-user", "password123");
        Integer modelId = createDeviceModel(accessToken, "AP8959", "APC");
        String locationCode = createRootLocation(accessToken, "Rack-01");

        mockMvc.perform(post("/api/manager/devices")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "modelId": %d,
                                  "locationNodeCode": "%s",
                                  "name": "PDU-좌",
                                  "description": "Rack-01 좌측 PDU",
                                  "enabled": true
                                }
                                """.formatted(modelId, locationCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.modelId").value(modelId))
                .andExpect(jsonPath("$.data.modelName").value("AP8959"))
                .andExpect(jsonPath("$.data.manufacturer").value("APC"))
                .andExpect(jsonPath("$.data.locationNodeName").value("Rack-01"))
                .andExpect(jsonPath("$.data.name").value("PDU-좌"))
                .andExpect(jsonPath("$.data.description").value("Rack-01 좌측 PDU"))
                .andExpect(jsonPath("$.data.enabled").value(true));
    }

    @Test
    void createDevice_withoutEnabled_defaultsToTrue() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "device-create-default-enabled", "password123");
        Integer modelId = createDeviceModel(accessToken, "LHT65N", "Dragino");
        String locationCode = createRootLocation(accessToken, "Zone-A");

        mockMvc.perform(post("/api/manager/devices")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "modelId": %d,
                                  "locationNodeCode": "%s",
                                  "name": "센서-01"
                                }
                                """.formatted(modelId, locationCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.description").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void createDevice_whenModelNotFound_returnsNotFound() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "device-create-model-nf", "password123");
        String locationCode = createRootLocation(accessToken, "Rack-NF");

        mockMvc.perform(post("/api/manager/devices")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "modelId": 999999,
                                  "locationNodeCode": "%s",
                                  "name": "PDU-좌"
                                }
                                """.formatted(locationCode)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("DeviceModel not found: 999999"));
    }

    @Test
    void createDevice_whenLocationNotFound_returnsNotFound() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "device-create-loc-nf", "password123");
        Integer modelId = createDeviceModel(accessToken, "AP8959", "APC");

        mockMvc.perform(post("/api/manager/devices")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "modelId": %d,
                                  "locationNodeCode": "UNKNOWN01",
                                  "name": "PDU-좌"
                                }
                                """.formatted(modelId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("LocationNode not found: UNKNOWN01"));
    }

    @Test
    void createDevice_withDuplicateNameAtSameLocation_returnsBadRequest() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "device-create-dup", "password123");
        Integer modelId = createDeviceModel(accessToken, "AP8959", "APC");
        String locationCode = createRootLocation(accessToken, "Rack-Dup");

        String body = """
                {
                  "modelId": %d,
                  "locationNodeCode": "%s",
                  "name": "PDU-좌"
                }
                """.formatted(modelId, locationCode);

        mockMvc.perform(post("/api/manager/devices")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/manager/devices")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("device name already exists at this location"));
    }

    @Test
    void createDevice_withBlankName_returnsBadRequest() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "device-create-blank-name", "password123");
        Integer modelId = createDeviceModel(accessToken, "AP8959", "APC");
        String locationCode = createRootLocation(accessToken, "Rack-Blank");

        mockMvc.perform(post("/api/manager/devices")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "modelId": %d,
                                  "locationNodeCode": "%s",
                                  "name": " "
                                }
                                """.formatted(modelId, locationCode)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid value for parameter 'name'"));
    }

    @Test
    void getDevice_returnsDevice() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "device-get-user", "password123");
        Integer modelId = createDeviceModel(accessToken, "AP8959", "APC");
        String locationCode = createRootLocation(accessToken, "Rack-Get");
        int deviceId = createDevice(accessToken, modelId, locationCode, "PDU-좌", "단건 조회용");

        mockMvc.perform(get("/api/manager/devices/{id}", deviceId)
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(deviceId))
                .andExpect(jsonPath("$.data.modelId").value(modelId))
                .andExpect(jsonPath("$.data.modelName").value("AP8959"))
                .andExpect(jsonPath("$.data.manufacturer").value("APC"))
                .andExpect(jsonPath("$.data.locationNodeName").value("Rack-Get"))
                .andExpect(jsonPath("$.data.name").value("PDU-좌"))
                .andExpect(jsonPath("$.data.description").value("단건 조회용"))
                .andExpect(jsonPath("$.data.enabled").value(true));
    }

    @Test
    void getDevice_whenNotFound_returnsNotFound() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "device-get-nf-user", "password123");

        mockMvc.perform(get("/api/manager/devices/{id}", 999999)
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Device not found: 999999"));
    }

    @Test
    void getDevices_returnsPagedList() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "device-list-user", "password123");
        Integer modelId = createDeviceModel(accessToken, "AP8959", "APC");
        String locationCode = createRootLocation(accessToken, "Rack-List");
        createDevice(accessToken, modelId, locationCode, "PDU-01", "d1");
        createDevice(accessToken, modelId, locationCode, "PDU-02", "d2");
        createDevice(accessToken, modelId, locationCode, "PDU-03", "d3");

        mockMvc.perform(get("/api/manager/devices")
                        .param("page", "1")
                        .param("size", "2")
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.content[0].name").value("PDU-01"))
                .andExpect(jsonPath("$.data.content[1].name").value("PDU-02"))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.first").value(true))
                .andExpect(jsonPath("$.data.last").value(false));

        mockMvc.perform(get("/api/manager/devices")
                        .param("page", "2")
                        .param("size", "2")
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].name").value("PDU-03"))
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.first").value(false))
                .andExpect(jsonPath("$.data.last").value(true));
    }

    @Test
    void getDevices_filtersByName() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "device-list-name-user", "password123");
        Integer modelId = createDeviceModel(accessToken, "AP8959", "APC");
        String locationCode = createRootLocation(accessToken, "Rack-NameFilter");
        createDevice(accessToken, modelId, locationCode, "PDU-좌", "left");
        createDevice(accessToken, modelId, locationCode, "센서-01", "sensor");

        mockMvc.perform(get("/api/manager/devices")
                        .param("name", "PDU")
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].name").value("PDU-좌"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void getDevices_filtersByEnabledAndLocation() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "device-list-enabled-user", "password123");
        Integer modelId = createDeviceModel(accessToken, "AP8959", "APC");
        String locationA = createRootLocation(accessToken, "Rack-A");
        String locationB = createRootLocation(accessToken, "Rack-B");
        createDevice(accessToken, modelId, locationA, "PDU-ON", "on", true);
        createDevice(accessToken, modelId, locationA, "PDU-OFF", "off", false);
        createDevice(accessToken, modelId, locationB, "PDU-OTHER", "other", false);

        mockMvc.perform(get("/api/manager/devices")
                        .param("locationNodeCode", locationA)
                        .param("enabled", "false")
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].name").value("PDU-OFF"))
                .andExpect(jsonPath("$.data.content[0].enabled").value(false))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void updateDevice_updatesFields() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "device-update-user", "password123");
        Integer modelId = createDeviceModel(accessToken, "AP8959", "APC");
        Integer newModelId = createDeviceModel(accessToken, "LHT65N", "Dragino");
        String locationCode = createRootLocation(accessToken, "Rack-Update");
        String newLocationCode = createRootLocation(accessToken, "Rack-Update-2");
        int deviceId = createDevice(accessToken, modelId, locationCode, "PDU-좌", "before");

        mockMvc.perform(put("/api/manager/devices/{id}", deviceId)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "modelId": %d,
                                  "locationNodeCode": "%s",
                                  "name": "센서-01",
                                  "description": "after",
                                  "enabled": false
                                }
                                """.formatted(newModelId, newLocationCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(deviceId))
                .andExpect(jsonPath("$.data.modelId").value(newModelId))
                .andExpect(jsonPath("$.data.modelName").value("LHT65N"))
                .andExpect(jsonPath("$.data.manufacturer").value("Dragino"))
                .andExpect(jsonPath("$.data.locationNodeName").value("Rack-Update-2"))
                .andExpect(jsonPath("$.data.name").value("센서-01"))
                .andExpect(jsonPath("$.data.description").value("after"))
                .andExpect(jsonPath("$.data.enabled").value(false));
    }

    @Test
    void updateDevice_whenNotFound_returnsNotFound() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "device-update-nf-user", "password123");
        Integer modelId = createDeviceModel(accessToken, "AP8959", "APC");
        String locationCode = createRootLocation(accessToken, "Rack-Update-NF");

        mockMvc.perform(put("/api/manager/devices/{id}", 999999)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "modelId": %d,
                                  "locationNodeCode": "%s",
                                  "name": "PDU-좌"
                                }
                                """.formatted(modelId, locationCode)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Device not found: 999999"));
    }

    @Test
    void updateDevice_withDuplicateNameAtSameLocation_returnsBadRequest() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "device-update-dup-user", "password123");
        Integer modelId = createDeviceModel(accessToken, "AP8959", "APC");
        String locationCode = createRootLocation(accessToken, "Rack-Update-Dup");
        createDevice(accessToken, modelId, locationCode, "PDU-좌", "first");
        int deviceId = createDevice(accessToken, modelId, locationCode, "PDU-우", "second");

        mockMvc.perform(put("/api/manager/devices/{id}", deviceId)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "modelId": %d,
                                  "locationNodeCode": "%s",
                                  "name": "PDU-좌"
                                }
                                """.formatted(modelId, locationCode)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("device name already exists at this location"));
    }

    @Test
    void updateDevice_keepsSameNameAtSameLocation() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "device-update-same-name", "password123");
        Integer modelId = createDeviceModel(accessToken, "AP8959", "APC");
        String locationCode = createRootLocation(accessToken, "Rack-Update-Same");
        int deviceId = createDevice(accessToken, modelId, locationCode, "PDU-좌", "before");

        mockMvc.perform(put("/api/manager/devices/{id}", deviceId)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "modelId": %d,
                                  "locationNodeCode": "%s",
                                  "name": "PDU-좌",
                                  "description": "updated desc"
                                }
                                """.formatted(modelId, locationCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("PDU-좌"))
                .andExpect(jsonPath("$.data.description").value("updated desc"));
    }

    @Test
    void updateDevice_whenModelChangeHasUnsupportedEndpoint_returnsConflict() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "device-update-endpoint-conflict", "password123");
        Integer snmpId = snmpProtocolTypeId(accessToken);
        Integer modbusId = modbusProtocolTypeId(accessToken);
        Integer snmpModelId = createDeviceModelWithProtocol(accessToken, "SNMP-ONLY", "APC", snmpId);
        Integer modbusModelId = createDeviceModelWithProtocol(accessToken, "MODBUS-ONLY", "Vendor", modbusId);
        String locationCode = createRootLocation(accessToken, "Rack-Model-Conflict");
        int deviceId = createDevice(accessToken, snmpModelId, locationCode, "PDU-좌", "before");
        createEndpoint(accessToken, deviceId, snmpId, "192.168.1.10", 161);

        mockMvc.perform(put("/api/manager/devices/{id}", deviceId)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "modelId": %d,
                                  "locationNodeCode": "%s",
                                  "name": "PDU-좌"
                                }
                                """.formatted(modbusModelId, locationCode)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error")
                        .value("device has endpoints not supported by new model: snmp"));
    }

    @Test
    void updateDevice_whenModelChangeWithMatchingEndpoint_succeeds() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "device-update-endpoint-ok", "password123");
        Integer snmpId = snmpProtocolTypeId(accessToken);
        Integer sourceModelId = createDeviceModelWithProtocol(accessToken, "AP8959-A", "APC", snmpId);
        Integer targetModelId = createDeviceModelWithProtocol(accessToken, "AP8959-B", "APC", snmpId);
        String locationCode = createRootLocation(accessToken, "Rack-Model-Ok");
        int deviceId = createDevice(accessToken, sourceModelId, locationCode, "PDU-좌", "before");
        createEndpoint(accessToken, deviceId, snmpId, "192.168.1.10", 161);

        mockMvc.perform(put("/api/manager/devices/{id}", deviceId)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "modelId": %d,
                                  "locationNodeCode": "%s",
                                  "name": "PDU-좌",
                                  "description": "after"
                                }
                                """.formatted(targetModelId, locationCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.modelId").value(targetModelId))
                .andExpect(jsonPath("$.data.description").value("after"));
    }

    @Test
    void updateDevice_whenModelChangeToDualProtocolModelWithSingleEndpoint_succeeds() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "device-update-dual-model", "password123");
        Integer snmpId = snmpProtocolTypeId(accessToken);
        Integer modbusId = modbusProtocolTypeId(accessToken);
        Integer snmpOnlyModelId = createDeviceModelWithProtocol(accessToken, "SNMP-ONLY-2", "APC", snmpId);
        Integer dualModelId = createDeviceModelWithProtocols(
                accessToken, "SNMP-MODBUS", "Vendor", snmpId, modbusId);
        String locationCode = createRootLocation(accessToken, "Rack-Dual-Model");
        int deviceId = createDevice(accessToken, snmpOnlyModelId, locationCode, "PDU-좌", "before");
        createEndpoint(accessToken, deviceId, snmpId, "192.168.1.10", 161);

        mockMvc.perform(put("/api/manager/devices/{id}", deviceId)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "modelId": %d,
                                  "locationNodeCode": "%s",
                                  "name": "PDU-좌"
                                }
                                """.formatted(dualModelId, locationCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.modelId").value(dualModelId));
    }

    @Test
    void createDevice_withoutModelId_returnsBadRequest() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "device-create-null-model", "password123");
        String locationCode = createRootLocation(accessToken, "Rack-NullModel");

        mockMvc.perform(post("/api/manager/devices")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "locationNodeCode": "%s",
                                  "name": "PDU-좌"
                                }
                                """.formatted(locationCode)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid value for parameter 'modelId'"));
    }

    @Test
    void deleteDevice_removesDevice() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "device-delete-user", "password123");
        Integer modelId = createDeviceModel(accessToken, "AP8959", "APC");
        String locationCode = createRootLocation(accessToken, "Rack-Delete");
        int deviceId = createDevice(accessToken, modelId, locationCode, "PDU-좌", "to delete");

        mockMvc.perform(delete("/api/manager/devices/{id}", deviceId)
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(deviceId));

        mockMvc.perform(get("/api/manager/devices/{id}", deviceId)
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Device not found: " + deviceId));
    }

    @Test
    void deleteDevice_whenNotFound_returnsNotFound() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "device-delete-nf-user", "password123");

        mockMvc.perform(delete("/api/manager/devices/{id}", 999999)
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Device not found: 999999"));
    }

    private int createDevice(
            String accessToken,
            Integer modelId,
            String locationCode,
            String name,
            String description
    ) throws Exception {
        return createDevice(accessToken, modelId, locationCode, name, description, true);
    }

    private int createDevice(
            String accessToken,
            Integer modelId,
            String locationCode,
            String name,
            String description,
            boolean enabled
    ) throws Exception {
        String response = mockMvc.perform(post("/api/manager/devices")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "modelId": %d,
                                  "locationNodeCode": "%s",
                                  "name": "%s",
                                  "description": "%s",
                                  "enabled": %s
                                }
                                """.formatted(modelId, locationCode, name, description, enabled)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).path("data").path("id").asInt();
    }

    private Integer snmpProtocolTypeId(String accessToken) throws Exception {
        return findOrCreateCommonCode(
                accessToken,
                findOrCreateCodeGroup(accessToken, "PROTOCOL_TYPE", "Protocol Type"),
                "snmp",
                "SNMP",
                1
        );
    }

    private Integer modbusProtocolTypeId(String accessToken) throws Exception {
        return findOrCreateCommonCode(
                accessToken,
                findOrCreateCodeGroup(accessToken, "PROTOCOL_TYPE", "Protocol Type"),
                "modbus",
                "Modbus",
                2
        );
    }

    private Integer createDeviceModelWithProtocol(
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

    private void createEndpoint(
            String accessToken,
            int deviceId,
            Integer protocolTypeId,
            String host,
            int port
    ) throws Exception {
        mockMvc.perform(post("/api/manager/devices/{deviceId}/endpoints", deviceId)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "protocolTypeId": %d,
                                  "host": "%s",
                                  "port": %d
                                }
                                """.formatted(protocolTypeId, host, port)))
                .andExpect(status().isOk());
    }

    private String createRootLocation(String accessToken, String name) throws Exception {
        Integer groupId = findOrCreateCodeGroup(accessToken, "LOCATION_TYPE", "Location Type");
        Integer rackTypeId = findOrCreateCommonCode(accessToken, groupId, "RACK", "랙", 3);
        return createLocationNode(accessToken, null, rackTypeId, name);
    }

    private Integer createDeviceModel(String accessToken, String name, String manufacturer) throws Exception {
        Integer modelTypeGroupId = findOrCreateCodeGroup(accessToken, "MODEL_TYPE", "Model Type");
        Integer deviceTypeId = findOrCreateCommonCode(accessToken, modelTypeGroupId, "PDU", "PDU", 1);
        Integer groupId = findOrCreateCodeGroup(accessToken, "PROTOCOL_TYPE", "Protocol Type");
        Integer snmpId = findOrCreateCommonCode(accessToken, groupId, "snmp", "SNMP", 1);

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
                                """.formatted(name, manufacturer, deviceTypeId, snmpId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).path("data").path("id").asInt();
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

    private Integer createCodeGroup(String accessToken, String groupKey, String groupName) throws Exception {
        return findOrCreateCodeGroup(accessToken, groupKey, groupName);
    }

    private Integer createCommonCode(
            String accessToken,
            Integer groupId,
            String code,
            String name,
            Integer sortOrder
    ) throws Exception {
        return findOrCreateCommonCode(accessToken, groupId, code, name, sortOrder);
    }

    private String createLocationNode(
            String accessToken,
            String parentCode,
            Integer locationTypeId,
            String name
    ) throws Exception {
        String parentJson = parentCode == null ? "null" : "\"%s\"".formatted(parentCode);
        String response = mockMvc.perform(post("/api/manager/location-node")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"parentCode": %s, "locationTypeId": %d, "name": "%s"}
                                """.formatted(parentJson, locationTypeId, name)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode codeNode = objectMapper.readTree(response).path("data").path("code");
        return codeNode.asText();
    }
}
