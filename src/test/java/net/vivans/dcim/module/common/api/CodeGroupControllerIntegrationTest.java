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
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ManagerServerApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class CodeGroupControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void create_returnCodeGroup() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "codegroup-user", "password123");

        mockMvc.perform(post("/api/manager/code-groups")
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"groupKey": "Test_Type", "groupName": "TEST"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.groupKey").value("Test_Type"))
                .andExpect(jsonPath("$.data.groupName").value("TEST"));
    }

    @Test
    void getCodeGroupList_returnCodeGroupList() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "codegroup-list-user", "password123");

        createCodeGroup(accessToken, "DEVICE_TYPE", "장비 유형");
        createCodeGroup(accessToken, "SENSOR_TYPE", "센서 유형");
        createCodeGroup(accessToken, "LOCATION_TYPE", "위치 유형");

        mockMvc.perform(get("/api/manager/code-groups")
                        .header("Authorization", bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(4))
                .andExpect(jsonPath("$.data[*].groupKey").value(hasItems("DEVICE_TYPE", "SENSOR_TYPE","LOCATION_TYPE")))
                .andExpect(jsonPath("$.data[*].groupName").value(hasItems("장비 유형", "센서 유형", "위치 유형")));
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

    @Test
    void createCodeGroup_returnUnauthorizedForWrongAccount() throws Exception {
        mockMvc.perform(post("/api/manager/code-groups")
                        .header("Authorization", bearerToken("invalid.access.token"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"groupKey": "Test_Type", "groupName": "TEST"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void createCodeGroup_returnEmptyCodeGroupRequest() throws Exception{
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "TEST", "TEST");
        mockMvc.perform(post("/api/manager/code-groups")
                .header("Authorization", bearerToken(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"groupKey": "", "groupName": ""}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void updateCodeGroup_returnUpdatedCodeGroup() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "codegroup-update-user", "password123");

        Integer id = createCodeGroup(accessToken, "OLD_TYPE", "이전 유형");

        mockMvc.perform(put("/api/manager/code-groups/{id}", id)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"groupKey": "NEW_TYPE", "groupName": "새 유형"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.groupKey").value("NEW_TYPE"))
                .andExpect(jsonPath("$.data.groupName").value("새 유형"));
    }

    @Test
    void updateCodeGroup_returnNotFoundWhenIdDoesNotExist() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "codegroup-update-notfound-user", "password123");

        mockMvc.perform(put("/api/manager/code-groups/{id}", 99999)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"groupKey": "NEW_TYPE", "groupName": "새 유형"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void updateCodeGroup_returnBadRequestForEmptyRequest() throws Exception {
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "codegroup-update-empty-user", "password123");

        Integer id = createCodeGroup(accessToken, "OLD_TYPE", "이전 유형");

        mockMvc.perform(put("/api/manager/code-groups/{id}", id)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"groupKey": "", "groupName": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void updateCodeGroup_returnDuplicationCodeGroup() throws Exception{
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "Duplication-user", "password123");

        createCodeGroup(accessToken, "DEVICE_TYPE", "장비 유형");
        Integer id = createCodeGroup(accessToken,"SENSOR_TYPE", "센서 유형");
        mockMvc.perform(put("/api/manager/code-groups/{id}", id)
                        .header("Authorization", bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"groupKey": "DEVICE_TYPE", "groupName": "센서 유형"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("400"))
                .andExpect(jsonPath("$.error").value("GroupKey already exists"));
    }

    @Test
    void createCodeGroup_returnWrongUrl() throws Exception{
        String accessToken = loginAndGetAccessToken(mockMvc, objectMapper, "TEST", "TEST");
        mockMvc.perform(post("/unKnown/url")
                .header("Authorization", bearerToken(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"groupKey": "TEST_Type", "groupName": "TEST"}
                        """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
