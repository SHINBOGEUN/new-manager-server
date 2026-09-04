package net.vivans.dcim.module.common.api;

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
import static org.hamcrest.CoreMatchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ManagerServerApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class CommonCodeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void create_returnCommonCodeResponse() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "common-code-user", "password123");

        Integer groupId = createCodeGroup(accessToken, "DEVICE_TYPE", "장비 유형");

        mockMvc.perform(post("/api/manager/common-codes")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"groupId": %d, "code": "pdu", "name": "pdu", "sortOrder": 1}
                                """.formatted(groupId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.groupId").value(groupId))
                .andExpect(jsonPath("$.data.code").value("pdu"))
                .andExpect(jsonPath("$.data.name").value("pdu"))
                .andExpect(jsonPath("$.data.sortOrder").value(1));
    }

    @Test
    void update_returnCommonCodeResponse() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "update-code-user", "password123");

        Integer groupId = createCodeGroup(accessToken, "DEVICE_TYPE", "장비 유형");
        Integer codeId = createCommonCode(accessToken, groupId, "pdu", "pdu", 1);

        mockMvc.perform(put("/api/manager/common-codes/{id}", codeId)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"groupId": %d, "code": "ups", "name": "ups", "sortOrder": 1}
                                """.formatted(groupId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(codeId))
                .andExpect(jsonPath("$.data.groupId").value(groupId))
                .andExpect(jsonPath("$.data.code").value("ups"))
                .andExpect(jsonPath("$.data.name").value("ups"))
                .andExpect(jsonPath("$.data.sortOrder").value(1));
    }

    @Test
    void get_returnCommonCodeListResponse() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "get-code-user", "password123");

        Integer groupId = createCodeGroup(accessToken, "DEVICE_TYPE", "장비 유형");
        createCommonCode(accessToken, groupId, "pdu", "pdu", 1);
        createCommonCode(accessToken, groupId, "ups", "ups", 1);

        mockMvc.perform(get("/api/manager/common-codes")
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(10))
                .andExpect(jsonPath("$.data[*].groupId").value(hasItems(groupId, groupId)))
                .andExpect(jsonPath("$.data[*].code").value(hasItems("pdu", "ups")))
                .andExpect(jsonPath("$.data[*].name").value(hasItems("pdu", "ups")));
    }

    @Test
    void get_withCodeGroupId_returnFilteredList() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "filter-code-user", "password123");

        Integer deviceGroupId = createCodeGroup(accessToken, "DEVICE_TYPE", "장비 유형");
        Integer locationGroupId = createCodeGroup(accessToken, "LOCATION_TYPE", "위치 유형");
        createCommonCode(accessToken, deviceGroupId, "pdu", "pdu", 1);
        createCommonCode(accessToken, deviceGroupId, "ups", "ups", 2);
        createCommonCode(accessToken, locationGroupId, "rack", "rack", 1);

        mockMvc.perform(get("/api/manager/common-codes")
                        .param("codeGroupId", deviceGroupId.toString())
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[*].groupId").value(hasItems(deviceGroupId, deviceGroupId)))
                .andExpect(jsonPath("$.data[*].code").value(hasItems("pdu", "ups")));
    }

    @Test
    void get_withUnknownCodeGroupId_returnNotFound() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "notfound-code-user", "password123");

        mockMvc.perform(get("/api/manager/common-codes")
                        .param("codeGroupId", "99999")
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void get_withCodeGroupId_whenNoCodes_returnEmptyList() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "empty-code-user", "password123");

        Integer groupId = createCodeGroup(accessToken, "DEVICE_TYPE", "장비 유형");

        mockMvc.perform(get("/api/manager/common-codes")
                        .param("codeGroupId", groupId.toString())
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
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

    private Integer createCommonCode(String accessToken, Integer groupId, String code, String name, Integer sortOrder) throws Exception {
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
