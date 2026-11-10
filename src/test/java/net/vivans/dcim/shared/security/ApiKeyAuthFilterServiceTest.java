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

    private static ApiKeyAuthFilter filter() {
        var filter = new ApiKeyAuthFilter();
        ReflectionTestUtils.setField(filter, "inboundServiceApiKey", "random-key");
        return filter;
    }

    private static MockHttpServletResponse call(ApiKeyAuthFilter filter, String method, String path, String key,
                                                MockFilterChain chain) throws Exception {
        var request = new MockHttpServletRequest(method, path);
        if (key != null) request.addHeader("X-Api-Key", key);
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        return response;
    }

    @Test void collectorKeyCanReadOnlyItsRequiredEndpoints() throws Exception {
        var chain = new MockFilterChain();
        var response = call(filter(), "GET", "/api/manager/mqtt/identities/bulk", "random-key", chain);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo("collector-service");
    }

    @Test void collectorKeyCannotAccessUnrelatedAuthenticatedApi() throws Exception {
        var chain = new MockFilterChain();
        var response = call(filter(), "GET", "/api/manager/users", "random-key", chain);
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    @Test void previousHardcodedKeyIsNotAccepted() throws Exception {
        var response = call(filter(), "GET", "/api/manager/mqtt/identities/bulk", "sensor-data-service", new MockFilterChain());
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test void alarmAndGpuInternalApisUseTheSameCollectorKey() throws Exception {
        for (String path : new String[]{"/api/internal/alarms/measurements", "/api/internal/gpu-agents/discover"}) {
            SecurityContextHolder.clearContext();
            var chain = new MockFilterChain();
            assertThat(call(filter(), "POST", path, "random-key", chain).getStatus()).isEqualTo(200);
            assertThat(chain.getRequest()).isNotNull();
            assertThat(call(filter(), "POST", path, null, new MockFilterChain()).getStatus()).isEqualTo(401);
            assertThat(call(filter(), "POST", path, "wrong", new MockFilterChain()).getStatus()).isEqualTo(401);
        }
    }

    @Test void blankConfiguredKeyRejectsEveryServiceCall() throws Exception {
        var filter = new ApiKeyAuthFilter();
        ReflectionTestUtils.setField(filter, "inboundServiceApiKey", "");
        assertThat(call(filter, "POST", "/api/internal/alarms/measurements", "", new MockFilterChain()).getStatus())
                .isEqualTo(401);
    }
}
