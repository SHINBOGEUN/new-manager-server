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
class DevicePageControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createAndListDevicePages_returnsLinkedPages() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "device-page-create", "password123");
        int deviceId = createDevice(accessToken, "PDU-page-1");
        Integer envId = devicePageCodeId(accessToken, "ENVIRONMENT", "Environment", 1);
        Integer analysisId = devicePageCodeId(accessToken, "ANALYSIS", "Analysis", 2);

        mockMvc.perform(post("/api/manager/devices/{deviceId}/pages", deviceId)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pageCodeId": %d}
                                """.formatted(envId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deviceId").value(deviceId))
                .andExpect(jsonPath("$.data.pageCode").value("ENVIRONMENT"));

        mockMvc.perform(post("/api/manager/devices/{deviceId}/pages", deviceId)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pageCodeId": %d}
                                """.formatted(analysisId)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/manager/devices/{deviceId}/pages", deviceId)
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    void createDevicePage_whenAlreadyLinked_returnsConflict() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "device-page-dup", "password123");
        int deviceId = createDevice(accessToken, "PDU-page-dup");
        Integer envId = devicePageCodeId(accessToken, "ENVIRONMENT", "Environment", 1);

        String body = """
                {"pageCodeId": %d}
                """.formatted(envId);
        mockMvc.perform(post("/api/manager/devices/{deviceId}/pages", deviceId)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/manager/devices/{deviceId}/pages", deviceId)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("page already linked to this device"));
    }

    @Test
    void createDevicePage_whenNotDevicePageGroup_returnsBadRequest() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "device-page-bad-group", "password123");
        int deviceId = createDevice(accessToken, "PDU-page-bad");
        Integer groupId = findOrCreateCodeGroup(accessToken, "PROTOCOL_TYPE", "Protocol Type");
        Integer snmpId = findOrCreateCommonCode(accessToken, groupId, "snmp", "SNMP", 1);

        mockMvc.perform(post("/api/manager/devices/{deviceId}/pages", deviceId)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pageCodeId": %d}
                                """.formatted(snmpId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("pageCode must belong to DEVICE_PAGE group"));
    }

    @Test
    void replaceDevicePages_replacesAll() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "device-page-put", "password123");
        int deviceId = createDevice(accessToken, "PDU-page-put");
        Integer envId = devicePageCodeId(accessToken, "ENVIRONMENT", "Environment", 1);
        Integer coolingId = devicePageCodeId(accessToken, "COOLING", "Cooling", 2);
        Integer analysisId = devicePageCodeId(accessToken, "ANALYSIS", "Analysis", 3);

        mockMvc.perform(post("/api/manager/devices/{deviceId}/pages", deviceId)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pageCodeId": %d}
                                """.formatted(envId)))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/manager/devices/{deviceId}/pages", deviceId)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pageCodeIds": [%d, %d]}
                                """.formatted(coolingId, analysisId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].pageCode").value("COOLING"))
                .andExpect(jsonPath("$.data[1].pageCode").value("ANALYSIS"));
    }

    @Test
    void deleteDevicePage_removesLink() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "device-page-del", "password123");
        int deviceId = createDevice(accessToken, "PDU-page-del");
        Integer envId = devicePageCodeId(accessToken, "ENVIRONMENT", "Environment", 1);

        String createResponse = mockMvc.perform(post("/api/manager/devices/{deviceId}/pages", deviceId)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pageCodeId": %d}
                                """.formatted(envId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        int pageId = objectMapper.readTree(createResponse).path("data").path("id").asInt();

        mockMvc.perform(delete("/api/manager/devices/{deviceId}/pages/{pageId}", deviceId, pageId)
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(pageId));

        mockMvc.perform(get("/api/manager/devices/{deviceId}/pages", deviceId)
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void getDevices_filterByPageCode_returnsMatchingDevices() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "device-page-filter", "password123");
        int envDeviceId = createDevice(accessToken, "Sensor-env");
        int otherDeviceId = createDevice(accessToken, "PDU-other");
        Integer envId = devicePageCodeId(accessToken, "ENVIRONMENT", "Environment", 1);

        mockMvc.perform(post("/api/manager/devices/{deviceId}/pages", envDeviceId)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pageCodeId": %d}
                                """.formatted(envId)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/manager/devices")
                        .param("pageCode", "ENVIRONMENT")
                        .param("name", "Sensor-env")
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].id").value(envDeviceId));

        mockMvc.perform(get("/api/manager/devices")
                        .param("pageCode", "ENVIRONMENT")
                        .param("name", "PDU-other")
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)));

        mockMvc.perform(get("/api/manager/devices/{id}", otherDeviceId)
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk());
    }

    private Integer devicePageCodeId(String accessToken, String code, String name, int sortOrder) throws Exception {
        Integer groupId = findOrCreateCodeGroup(accessToken, "DEVICE_PAGE", "Device Page");
        return findOrCreateCommonCode(accessToken, groupId, code, name, sortOrder);
    }

    private int createDevice(String accessToken, String name) throws Exception {
        Integer protocolGroupId = findOrCreateCodeGroup(accessToken, "PROTOCOL_TYPE", "Protocol Type");
        Integer snmpId = findOrCreateCommonCode(accessToken, protocolGroupId, "snmp", "SNMP", 1);
        Integer modelTypeGroupId = findOrCreateCodeGroup(accessToken, "MODEL_TYPE", "Model Type");
        Integer deviceTypeId = findOrCreateCommonCode(accessToken, modelTypeGroupId, "PDU", "PDU", 1);

        String modelResponse = mockMvc.perform(post("/api/manager/device-models")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Model-%s",
                                  "manufacturer": "APC",
                                  "deviceTypeId": %d,
                                  "protocols": [ { "protocolTypeId": %d } ]
                                }
                                """.formatted(name, deviceTypeId, snmpId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        int modelId = objectMapper.readTree(modelResponse).path("data").path("id").asInt();

        Integer locationGroupId = findOrCreateCodeGroup(accessToken, "LOCATION_TYPE", "Location Type");
        Integer rackTypeId = findOrCreateCommonCode(accessToken, locationGroupId, "RACK", "랙", 3);
        String locationResponse = mockMvc.perform(post("/api/manager/location-node")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"parentCode": null, "locationTypeId": %d, "name": "Rack-%s"}
                                """.formatted(rackTypeId, name)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String locationCode = objectMapper.readTree(locationResponse).path("data").path("code").asText();

        String deviceResponse = mockMvc.perform(post("/api/manager/devices")
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

        return objectMapper.readTree(deviceResponse).path("data").path("id").asInt();
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
