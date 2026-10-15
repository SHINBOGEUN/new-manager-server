package net.vivans.dcim.module.mqtt.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.vivans.dcim.bootstrap.ManagerServerApplication;
import net.vivans.dcim.module.identity.domain.repository.UserRepository;
import net.vivans.dcim.module.mqtt.domain.repository.MqttSourceStatusRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import java.util.UUID;
import static net.vivans.dcim.support.AuthTestSupport.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = ManagerServerApplication.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:mqtt_sources;DB_CLOSE_DELAY=-1", "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false", "sensor-data.service.enabled=false", "collector.service.enabled=false", "influx.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("local")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class MqttSourceIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository users;
    @Autowired MqttSourceStatusRepository statuses;
    private String auth;

    @BeforeEach
    void login() throws Exception {
        auth = bearerToken(loginAndGetAccessToken(mvc, mapper, users, "mqtt-source-" + UUID.randomUUID(), "password123"));
    }

    private String body(String name, String type, String topic, boolean enabled) {
        return """
                {"name":"%s","sourceType":"%s","brokerUrl":"tcp://mqtt.example:1883",
                 "topic":"%s","credentialKey":"gpu-key","enabled":%s}
                """.formatted(name, type, topic, enabled);
    }

    private int create(String type, boolean enabled) throws Exception {
        var result = mvc.perform(post("/api/manager/mqtt/sources").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body(UUID.randomUUID().toString(), type, "devices/+/telemetry", enabled)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.sourceType").value(type))
                .andExpect(jsonPath("$.data.configVersion").value(1)).andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asInt();
    }

    @Test
    void agentCrudRetainsIdIncrementsVersionAndStoresStatus() throws Exception {
        int id = create("AGENT", true);
        mvc.perform(put("/api/manager/mqtt/sources/{id}", id).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body("Updated " + id, "AGENT", "gpu/+/telemetry", false)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.configVersion").value(2)).andExpect(jsonPath("$.data.enabled").value(false));
        mvc.perform(post("/api/manager/mqtt/sources/{id}/status", id).header("X-Api-Key", "sensor-data-service")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CONNECTED\",\"messageCount\":10}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.messageCount").value(10));
        mvc.perform(get("/api/manager/mqtt/sources/bulk").header("X-Api-Key", "sensor-data-service"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[?(@.id == " + id + ")].enabled").value(org.hamcrest.Matchers.contains(false)));
        mvc.perform(delete("/api/manager/mqtt/sources/{id}", id).header("Authorization", auth)).andExpect(status().isOk());
        assertThat(statuses.findBySourceId(id)).isEmpty();
    }

    @Test
    void legacyBulkCannotSendAgentsToOldLoraSubscriber() throws Exception {
        int agent = create("AGENT", true);
        int lora = create("CHIRPSTACK_LORA", false);
        var result = mvc.perform(get("/api/manager/lora/sources/bulk").header("X-Api-Key", "sensor-data-service"))
                .andExpect(status().isOk()).andReturn();
        var data = mapper.readTree(result.getResponse().getContentAsString()).path("data");
        assertThat(data.findValuesAsText("id")).contains(Integer.toString(lora)).doesNotContain(Integer.toString(agent));
        mvc.perform(post("/api/manager/lora/sources/{id}/status", agent).header("X-Api-Key", "sensor-data-service")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CONNECTED\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/manager/lora/sources/{id}/status", lora).header("X-Api-Key", "sensor-data-service")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DISABLED\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsInvalidFiltersCredentialsAndUnauthenticatedCalls() throws Exception {
        mvc.perform(get("/api/manager/mqtt/sources/bulk")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/manager/mqtt/sources").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body("bad filter", "AGENT", "a/#/b", true)))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/manager/mqtt/sources").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body("bad broker", "AGENT", "a/+", true)
                                .replace("tcp://mqtt.example:1883", "tcp://user:secret@mqtt.example:1883")))
                .andExpect(status().isBadRequest());
    }
}
