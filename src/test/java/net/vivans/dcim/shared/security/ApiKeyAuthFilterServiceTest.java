package net.vivans.dcim.shared.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class ApiKeyAuthFilterServiceTest {
    @AfterEach void clearContext() { SecurityContextHolder.clearContext(); }

    @Test void configuredSensorKeyCanReadOnlyItsRequiredEndpoints() throws Exception {
        var filter = new ApiKeyAuthFilter();
        ReflectionTestUtils.setField(filter, "managerSensorDataApiKey", "random-key");
        var request = new MockHttpServletRequest("GET", "/api/manager/mqtt/identities/bulk");
        request.addHeader("X-Api-Key", "random-key");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo("sensor-data-service");
    }

    @Test void sensorKeyCannotAccessUnrelatedAuthenticatedApi() throws Exception {
        var filter = new ApiKeyAuthFilter();
        ReflectionTestUtils.setField(filter, "managerSensorDataApiKey", "random-key");
        var request = new MockHttpServletRequest("GET", "/api/manager/users");
        request.addHeader("X-Api-Key", "random-key");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    @Test void previousHardcodedKeyIsNotAccepted() throws Exception {
        var filter = new ApiKeyAuthFilter();
        ReflectionTestUtils.setField(filter, "managerSensorDataApiKey", "random-key");
        var request = new MockHttpServletRequest("GET", "/api/manager/mqtt/identities/bulk");
        request.addHeader("X-Api-Key", "sensor-data-service");
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        assertThat(response.getStatus()).isEqualTo(401);
    }
}
