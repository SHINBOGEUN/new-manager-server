package net.vivans.dcim.module.devicegroup.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.vivans.dcim.module.identity.domain.repository.UserRepository;
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
import static org.hamcrest.Matchers.hasItem;
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
class DeviceGroupControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    void createUpdateListAndDeleteDeviceGroup() throws Exception {
        String token = loginAndGetAccessToken(mockMvc, objectMapper, userRepository, "device-group-user", "password123");

        String created = mockMvc.perform(post("/api/manager/device-groups")
                        .header("Authorization", bearerToken(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"IT 장비","description":"서버실 IT 전력 대상","enabled":true,"deviceIds":[]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("IT 장비"))
                .andExpect(jsonPath("$.data.deviceCount").value(0))
                .andReturn().getResponse().getContentAsString();
        int id = objectMapper.readTree(created).path("data").path("id").asInt();

        mockMvc.perform(put("/api/manager/device-groups/{id}", id)
                        .header("Authorization", bearerToken(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Cooling 장비","description":"냉각 전력 대상","enabled":false,"deviceIds":[]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Cooling 장비"))
                .andExpect(jsonPath("$.data.enabled").value(false));

        mockMvc.perform(get("/api/manager/device-groups")
                        .header("Authorization", bearerToken(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].name", hasItem("Cooling 장비")));

        mockMvc.perform(delete("/api/manager/device-groups/{id}", id)
                        .header("Authorization", bearerToken(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(id));
    }
}
