package net.vivans.dcim.module.location.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.vivans.dcim.module.identity.domain.repository.UserRepository;
import net.vivans.dcim.bootstrap.ManagerServerApplication;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.location.domain.model.LocationNode;
import net.vivans.dcim.module.location.domain.repository.LocationNodeRepository;
import net.vivans.dcim.module.common.domain.repository.CommonCodeRepository;
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
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ManagerServerApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class LocationNodeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LocationNodeRepository locationNodeRepository;

    @Autowired
    private CommonCodeRepository commonCodeRepository;

    @Test
    void createAndGetTree_returnsNestedChildren() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, userRepository, "location-tree-user", "password123");
        Integer groupId = createCodeGroup(accessToken, "LOCATION_TYPE", "위치 유형");
        Integer containerTypeId = createCommonCode(accessToken, groupId, "CONTAINER", "컨테이너", 1);
        Integer rowTypeId = createCommonCode(accessToken, groupId, "ROW", "열", 2);

        String rootCode = createLocationNode(accessToken, null, containerTypeId, "컨테이너 A");
        createLocationNode(accessToken, rootCode, rowTypeId, "A열");

        mockMvc.perform(get("/api/manager/location-node")
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].code").value(rootCode))
                .andExpect(jsonPath("$.data[0].name").value("컨테이너 A"))
                .andExpect(jsonPath("$.data[0].children", hasSize(1)))
                .andExpect(jsonPath("$.data[0].children[0].parentCode").value(rootCode))
                .andExpect(jsonPath("$.data[0].children[0].name").value("A열"));
    }

    @Test
    void createIntermediateType_reparentsExistingChildren() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, userRepository, "location-reparent-user", "password123");
        Integer groupId = createCodeGroup(accessToken, "LOCATION_TYPE", "위치 유형");
        Integer containerTypeId = createCommonCode(accessToken, groupId, "CONTAINER", "컨테이너", 1);
        Integer zoneTypeId = createCommonCode(accessToken, groupId, "ZONE", "존", 2);
        Integer rowTypeId = createCommonCode(accessToken, groupId, "ROW", "열", 3);

        String rootCode = createLocationNode(accessToken, null, containerTypeId, "컨테이너 A");
        String rowCode = createLocationNode(accessToken, rootCode, rowTypeId, "A열");
        String zoneCode = createLocationNode(accessToken, rootCode, zoneTypeId, "존 1");

        mockMvc.perform(get("/api/manager/location-node")
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value(rootCode))
                .andExpect(jsonPath("$.data[0].children", hasSize(1)))
                .andExpect(jsonPath("$.data[0].children[0].code").value(zoneCode))
                .andExpect(jsonPath("$.data[0].children[0].children", hasSize(1)))
                .andExpect(jsonPath("$.data[0].children[0].children[0].code").value(rowCode))
                .andExpect(jsonPath("$.data[0].children[0].children[0].parentCode").value(zoneCode));
    }

    @Test
    void create_invalidLocationTypeOrder_returnsBadRequest() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, userRepository, "location-type-order-user", "password123");
        Integer groupId = createCodeGroup(accessToken, "LOCATION_TYPE", "위치 유형");
        Integer containerTypeId = createCommonCode(accessToken, groupId, "CONTAINER", "컨테이너", 1);

        String rootCode = createLocationNode(accessToken, null, containerTypeId, "컨테이너 A");

        mockMvc.perform(post("/api/manager/location-node")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"parentCode": "%s", "locationTypeId": %d, "name": "잘못된 자식"}
                                """.formatted(rootCode, containerTypeId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void update_updatesNameAndLocationType() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, userRepository, "location-update-user", "password123");
        Integer groupId = createCodeGroup(accessToken, "LOCATION_TYPE", "위치 유형");
        Integer containerTypeId = createCommonCode(accessToken, groupId, "CONTAINER", "컨테이너", 1);
        Integer zoneTypeId = createCommonCode(accessToken, groupId, "ZONE", "존", 2);

        String rootCode = createLocationNode(accessToken, null, containerTypeId, "컨테이너 A");

        mockMvc.perform(put("/api/manager/location-node/{code}", rootCode)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"locationTypeId": %d, "name": "컨테이너 A (수정)"}
                                """.formatted(zoneTypeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.code").value(rootCode))
                .andExpect(jsonPath("$.data.name").value("컨테이너 A (수정)"))
                .andExpect(jsonPath("$.data.locationTypeId").value(zoneTypeId));
    }

    @Test
    void updateParent_promotesChildToRoot() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, userRepository, "location-parent-user", "password123");
        Integer groupId = createCodeGroup(accessToken, "LOCATION_TYPE", "위치 유형");
        Integer containerTypeId = createCommonCode(accessToken, groupId, "CONTAINER", "컨테이너", 1);
        Integer rowTypeId = createCommonCode(accessToken, groupId, "ROW", "열", 2);

        String rootCode = createLocationNode(accessToken, null, containerTypeId, "컨테이너 A");
        String rowCode = createLocationNode(accessToken, rootCode, rowTypeId, "A열");

        mockMvc.perform(patch("/api/manager/location-node/{code}/parent", rowCode)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"parentCode": null}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.code").value(rowCode))
                .andExpect(jsonPath("$.data.parentCode").value(nullValue()));
    }

    @Test
    void create_duplicateNameUnderSameParent_returnsBadRequest() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, userRepository, "location-dup-user", "password123");
        Integer groupId = createCodeGroup(accessToken, "LOCATION_TYPE", "위치 유형");
        Integer containerTypeId = createCommonCode(accessToken, groupId, "CONTAINER", "컨테이너", 1);

        createLocationNode(accessToken, null, containerTypeId, "컨테이너 A");

        mockMvc.perform(post("/api/manager/location-node")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"parentCode": null, "locationTypeId": %d, "name": "컨테이너 A"}
                                """.formatted(containerTypeId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void bulkCreate_registersTreeWithChildren() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, userRepository, "location-bulk-user", "password123");
        Integer groupId = createCodeGroup(accessToken, "LOCATION_TYPE", "위치 유형");
        Integer containerTypeId = createCommonCode(accessToken, groupId, "CONTAINER", "컨테이너", 1);
        Integer rowTypeId = createCommonCode(accessToken, groupId, "ROW", "열", 2);

        String response = mockMvc.perform(post("/api/manager/location-node/bulk")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "parentCode": null,
                                  "nodes": [
                                    {
                                      "locationTypeId": %d,
                                      "name": "컨테이너 A",
                                      "children": [
                                        {
                                          "locationTypeId": %d,
                                          "name": "A열",
                                          "children": []
                                        }
                                      ]
                                    }
                                  ]
                                }
                                """.formatted(containerTypeId, rowTypeId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(response).path("data").get(0);
        String rootCode = root.path("code").asText();

        mockMvc.perform(get("/api/manager/location-node")
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value(rootCode))
                .andExpect(jsonPath("$.data[0].name").value("컨테이너 A"))
                .andExpect(jsonPath("$.data[0].children", hasSize(1)))
                .andExpect(jsonPath("$.data[0].children[0].name").value("A열"))
                .andExpect(jsonPath("$.data[0].children[0].parentCode").value(rootCode));
    }

    @Test
    void deleteLeafNode_removesNode() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, userRepository, "location-delete-user", "password123");
        Integer groupId = createCodeGroup(accessToken, "LOCATION_TYPE", "위치 유형");
        Integer containerTypeId = createCommonCode(accessToken, groupId, "CONTAINER", "컨테이너", 1);
        Integer rowTypeId = createCommonCode(accessToken, groupId, "ROW", "열", 2);

        String rootCode = createLocationNode(accessToken, null, containerTypeId, "컨테이너 A");
        String rowCode = createLocationNode(accessToken, rootCode, rowTypeId, "A열");

        mockMvc.perform(delete("/api/manager/location-node/{code}", rowCode)
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deletedCode").value(rowCode))
                .andExpect(jsonPath("$.data.reassignedDeviceCount").value(0));

        mockMvc.perform(get("/api/manager/location-node")
                        .param("parentCode", rootCode)
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].code").value(rootCode))
                .andExpect(jsonPath("$.data[0].children", hasSize(0)));
    }

    @Test
    void deleteNodeWithChildren_returnsBadRequest() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, userRepository, "location-delete-parent-user", "password123");
        Integer groupId = createCodeGroup(accessToken, "LOCATION_TYPE", "위치 유형");
        Integer containerTypeId = createCommonCode(accessToken, groupId, "CONTAINER", "컨테이너", 1);
        Integer rowTypeId = createCommonCode(accessToken, groupId, "ROW", "열", 2);

        String rootCode = createLocationNode(accessToken, null, containerTypeId, "컨테이너 A");
        createLocationNode(accessToken, rootCode, rowTypeId, "A열");

        mockMvc.perform(delete("/api/manager/location-node/{code}", rootCode)
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void deleteSubtree_removesNodeAndDescendants() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, userRepository, "location-delete-subtree-user", "password123");
        Integer groupId = createCodeGroup(accessToken, "LOCATION_TYPE", "위치 유형");
        Integer containerTypeId = createCommonCode(accessToken, groupId, "CONTAINER", "컨테이너", 1);
        Integer rowTypeId = createCommonCode(accessToken, groupId, "ROW", "열", 2);

        String rootCode = createLocationNode(accessToken, null, containerTypeId, "컨테이너 A");
        createLocationNode(accessToken, rootCode, rowTypeId, "A열");

        mockMvc.perform(delete("/api/manager/location-node/{code}/subtree", rootCode)
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deletedCode").value(rootCode))
                .andExpect(jsonPath("$.data.reassignedDeviceCount").value(0));

        mockMvc.perform(get("/api/manager/location-node")
                        .param("parentCode", rootCode)
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteLeafNode_reassignsReferencedDevicesToUnassigned() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, userRepository, "location-delete-device-user", "password123");
        ensureUnassignedLocationNode(accessToken);
        Integer groupId = findOrCreateCodeGroup(accessToken, "LOCATION_TYPE", "Location Type");
        Integer rackTypeId = createCommonCode(accessToken, groupId, "RACK", "랙", 3);
        Integer modelTypeGroupId = createCodeGroup(accessToken, "MODEL_TYPE", "Model Type");
        Integer deviceTypeId = createCommonCode(accessToken, modelTypeGroupId, "PDU", "PDU", 1);
        Integer protocolGroupId = createCodeGroup(accessToken, "PROTOCOL_TYPE", "Protocol Type");
        Integer snmpId = createCommonCode(accessToken, protocolGroupId, "snmp", "SNMP", 1);

        String rackCode = createLocationNode(accessToken, null, rackTypeId, "Rack-Device-Delete");
        Integer modelId = createDeviceModel(accessToken, deviceTypeId, snmpId, "AP8959", "APC");
        int deviceId = createDevice(accessToken, modelId, rackCode, "PDU-좌", "delete test");

        mockMvc.perform(delete("/api/manager/location-node/{code}", rackCode)
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deletedCode").value(rackCode))
                .andExpect(jsonPath("$.data.reassignedDeviceCount").value(1));

        mockMvc.perform(get("/api/manager/devices/{id}", deviceId)
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.locationNodeName").value("미배정"));
    }

    @Test
    void deleteSubtree_whenDeviceNamesConflictAtUnassigned_returnsConflict() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, userRepository, "location-delete-conflict-user", "password123");
        ensureUnassignedLocationNode(accessToken);
        Integer groupId = findOrCreateCodeGroup(accessToken, "LOCATION_TYPE", "Location Type");
        Integer containerTypeId = createCommonCode(accessToken, groupId, "CONTAINER", "컨테이너", 1);
        Integer rowTypeId = createCommonCode(accessToken, groupId, "ROW", "열", 2);
        Integer modelTypeGroupId = createCodeGroup(accessToken, "MODEL_TYPE", "Model Type");
        Integer deviceTypeId = createCommonCode(accessToken, modelTypeGroupId, "PDU", "PDU", 1);
        Integer protocolGroupId = createCodeGroup(accessToken, "PROTOCOL_TYPE", "Protocol Type");
        Integer snmpId = createCommonCode(accessToken, protocolGroupId, "snmp", "SNMP", 1);

        Integer modelId = createDeviceModel(accessToken, deviceTypeId, snmpId, "AP8959", "APC");
        String containerCode = createLocationNode(accessToken, null, containerTypeId, "Container-Conflict");
        String rowCodeA = createLocationNode(accessToken, containerCode, rowTypeId, "Row-A");
        String rowCodeB = createLocationNode(accessToken, containerCode, rowTypeId, "Row-B");
        createDevice(accessToken, modelId, rowCodeA, "PDU-좌", "row a");
        createDevice(accessToken, modelId, rowCodeB, "PDU-좌", "row b");

        mockMvc.perform(delete("/api/manager/location-node/{code}/subtree", containerCode)
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(
                        "device name conflict at UNASSIGNED; rename devices before deleting location"));
    }

    @Test
    void deleteUnassignedNode_returnsConflict() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, userRepository, "location-delete-unassigned-user", "password123");

        mockMvc.perform(delete("/api/manager/location-node/{code}", "UNASSIGNED")
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("cannot delete system location node"));
    }

    @Test
    void get_withUnknownParentCode_returnsNotFound() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, userRepository, "location-notfound-user", "password123");

        mockMvc.perform(get("/api/manager/location-node")
                        .param("parentCode", "UNKNOWN01")
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    private void ensureUnassignedLocationNode(String accessToken) throws Exception {
        Integer groupId = findOrCreateCodeGroup(accessToken, "LOCATION_TYPE", "Location Type");
        Integer unassignedTypeId = findOrCreateCommonCode(accessToken, groupId, "UNASSIGNED", "미배정", -1);
        if (!locationNodeRepository.existsByCode(Device.UNASSIGNED_LOCATION_CODE)) {
            CommonCode unassignedType = commonCodeRepository.findById(unassignedTypeId).orElseThrow();
            locationNodeRepository.save(
                    LocationNode.createRoot(Device.UNASSIGNED_LOCATION_CODE, unassignedType, "미배정")
            );
        }
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

        return createCodeGroup(accessToken, groupKey, groupName);
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

        return createCommonCode(accessToken, groupId, code, name, sortOrder);
    }

    private Integer createCodeGroup(String accessToken, String groupKey, String groupName) throws Exception {
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

    private Integer createCommonCode(
            String accessToken,
            Integer groupId,
            String code,
            String name,
            Integer sortOrder
    ) throws Exception {
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

    private Integer createDeviceModel(
            String accessToken,
            Integer deviceTypeId,
            Integer protocolTypeId,
            String name,
            String manufacturer
    ) throws Exception {
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

    private int createDevice(
            String accessToken,
            Integer modelId,
            String locationCode,
            String name,
            String description
    ) throws Exception {
        String response = mockMvc.perform(post("/api/manager/devices")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "modelId": %d,
                                  "locationNodeCode": "%s",
                                  "name": "%s",
                                  "description": "%s"
                                }
                                """.formatted(modelId, locationCode, name, description)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).path("data").path("id").asInt();
    }
}
