package net.vivans.dcim.module.mqtt.api;

import java.util.List;
import java.util.Optional;
import net.vivans.dcim.module.mqtt.api.dto.MqttIdentityLookupResponse;
import net.vivans.dcim.module.mqtt.application.MqttLookupService;
import net.vivans.dcim.module.mqtt.domain.model.MqttIdentityType;
import net.vivans.dcim.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MqttLookupControllerTest {
    private final MqttLookupService service = mock(MqttLookupService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new MqttLookupController(service))
            .setControllerAdvice(new GlobalExceptionHandler()).build();

    @Test
    void resolveReturnsCommonWrapperAndIdentityFields() throws Exception {
        when(service.resolve(MqttIdentityType.MAC, "aabbccddeeff")).thenReturn(Optional.of(
                new MqttIdentityLookupResponse(12, MqttIdentityType.MAC, "aabbccddeeff", "AABBCCDDEEFF",
                        40, "GPU 1", 8, "GPU", true)));
        mvc.perform(get("/api/manager/mqtt/identities/resolve")
                        .param("identityType", "MAC").param("externalId", "aabbccddeeff"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data.identityId").value(12))
                .andExpect(jsonPath("$.data.deviceId").value(40));
    }

    @Test
    void missingIdentityReturnsNullAndBulkEndpointsReturnArrays() throws Exception {
        when(service.resolve(MqttIdentityType.SERIAL, "missing")).thenReturn(Optional.empty());
        when(service.getAllEnabledIdentities()).thenReturn(List.of());
        when(service.getAllEnabledModelMappings()).thenReturn(List.of());
        mvc.perform(get("/api/manager/mqtt/identities/resolve")
                        .param("identityType", "SERIAL").param("externalId", "missing"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").doesNotExist());
        mvc.perform(get("/api/manager/mqtt/identities/bulk"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").isArray());
        mvc.perform(get("/api/manager/mqtt/mappings/models/bulk"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void invalidTypeMissingParameterAndMalformedIdentityReturn400() throws Exception {
        mvc.perform(get("/api/manager/mqtt/identities/resolve")
                        .param("identityType", "INVALID").param("externalId", "x"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/manager/mqtt/identities/resolve").param("identityType", "MAC"))
                .andExpect(status().isBadRequest());
        when(service.resolve(MqttIdentityType.MAC, "x")).thenThrow(new IllegalArgumentException("Invalid MAC"));
        mvc.perform(get("/api/manager/mqtt/identities/resolve")
                        .param("identityType", "MAC").param("externalId", "x"))
                .andExpect(status().isBadRequest());
    }
}
