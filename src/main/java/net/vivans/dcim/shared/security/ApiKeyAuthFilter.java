package net.vivans.dcim.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Slf4j
@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {
    private final List<String> validApiKeys = List.of("collector-service", "sensor-data-service");

    @Value("${external-data.api-key:}")
    private String externalDataApiKey;

    @Value("${sensor-data.service.api-key:sensor-data-server}")
    private String sensorDataApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String apiKey = request.getHeader("X-Api-Key");
        String path = request.getRequestURI();

        if (path.equals("/api/internal/alarms/measurements")) {
            if (!matchesKey(sensorDataApiKey, apiKey)) {
                reject(response);
                return;
            }
            authenticateApiKey("sensor-data-service");
            filterChain.doFilter(request, response);
            return;
        }

        if (requiresExternalDataApiKey(path, apiKey)) {
            if (!matchesExternalDataApiKey(apiKey)) {
                reject(response);
                return;
            }
            authenticateApiKey("external-data-service");
            filterChain.doFilter(request, response);
            return;
        }

        if (apiKey != null) {
            if (validApiKeys.contains(apiKey)) {
                authenticateApiKey("admin");
                log.debug("API Key 인증 성공");
            } else {
                log.warn("유효하지 않은 API Key");
                reject(response);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private static boolean requiresExternalDataApiKey(String path, String apiKey) {
        if (path.equals("/api/internal/external-data") || path.startsWith("/api/internal/external-data/")) {
            return true;
        }
        boolean electricityQueryPath = path.startsWith("/api/manager/electricity-bill/")
                || path.startsWith("/api/manager/power-usage/");
        return electricityQueryPath && apiKey != null;
    }

    private boolean matchesExternalDataApiKey(String providedApiKey) {
        return matchesKey(externalDataApiKey, providedApiKey);
    }

    private static boolean matchesKey(String expected, String providedApiKey) {
        if (expected == null || expected.isBlank() || providedApiKey == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                providedApiKey.getBytes(StandardCharsets.UTF_8));
    }

    private static void authenticateApiKey(String principal) {
        var auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private static void reject(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Invalid API Key\"}");
    }
}
