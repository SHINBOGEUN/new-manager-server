package net.vivans.dcim.shared.security;

import net.vivans.dcim.shared.exception.JwtAccessDeniedHandler;
import net.vivans.dcim.shared.exception.JwtAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.web.ServletTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SceneModuleSecurityTest.ProtectedController.class,
        properties = "security.auth-enabled=true")
@ContextConfiguration(classes = SceneModuleSecurityTest.TestConfig.class)
@TestExecutionListeners(listeners = {ServletTestExecutionListener.class, DependencyInjectionTestExecutionListener.class},
        mergeMode = TestExecutionListeners.MergeMode.REPLACE_DEFAULTS)
class SceneModuleSecurityTest {
    @Autowired MockMvc mvc;
    @Configuration
    @EnableAutoConfiguration
    @Import({SecurityConfig.class, ProtectedController.class})
    static class TestConfig {
        @Bean JwtAuthenticationFilter jwtAuthenticationFilter() { return new JwtAuthenticationFilter(null, null); }
        @Bean ApiKeyAuthFilter apiKeyAuthFilter() { return new ApiKeyAuthFilter(); }
        @Bean JwtAccessDeniedHandler jwtAccessDeniedHandler() { return new JwtAccessDeniedHandler(); }
        @Bean JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint() { return new JwtAuthenticationEntryPoint(); }
    }

    @RestController
    static class ProtectedController {
        @GetMapping("/api/manager/scene-security-check") String secure() { return "protected"; }
    }

    @Test void sceneModuleLoadsWithoutBearerTokenWhileApiRemainsProtected() throws Exception {
        mvc.perform(get("/page-scene-3d.js")).andExpect(status().isOk());
        mvc.perform(get("/scene-group-lock.js")).andExpect(status().isOk());
        mvc.perform(get("/rack-widget-layout.js")).andExpect(status().isOk());
        mvc.perform(get("/rack-widget-chart.js")).andExpect(status().isOk());
        mvc.perform(get("/rack-door-attachment.js")).andExpect(status().isOk());
        mvc.perform(get("/rack-scene-display.js")).andExpect(status().isOk());
        mvc.perform(get("/api/manager/scene-security-check")).andExpect(status().isUnauthorized());
    }
}
