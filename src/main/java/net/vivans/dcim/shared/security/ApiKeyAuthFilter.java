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
import java.util.regex.Pattern;

/**
 * 서비스 간 X-Api-Key 인증.
 * <ul>
 *   <li>Collector(MQTT 수신 포함) -> Manager: {@code MANAGER_API_KEY} 하나만 사용하고, 아래 {@link #COLLECTOR_ROUTES}에만 허용한다.</li>
 *   <li>외부 데이터 연동: {@code EXTERNAL_DATA_API_KEY}.</li>
 * </ul>
 * 사용자 화면은 서비스 키가 아니라 JWT로 인증한다.
 */
@Slf4j
@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    static final String COLLECTOR_PRINCIPAL = "collector-service";

    /** Collector가 호출하는 Manager API. 새 호출을 추가하면 이 목록과 Collector 코드를 함께 바꾼다. */
    static final List<ServiceRoute> COLLECTOR_ROUTES = List.of(
            ServiceRoute.of("GET", "/api/manager/mqtt/identities/bulk"),
            ServiceRoute.of("GET", "/api/manager/mqtt/identities/resolve"),
            ServiceRoute.of("GET", "/api/manager/mqtt/mappings/models/bulk"),
            ServiceRoute.of("GET", "/api/manager/mqtt/sources/bulk"),
            ServiceRoute.of("GET", "/api/manager/lora/sources/bulk"),
            ServiceRoute.of("GET", "/api/manager/devices/[0-9]+"),
            ServiceRoute.of("POST", "/api/manager/(mqtt|lora)/sources/[0-9]+/status"),
            ServiceRoute.of("POST", "/api/internal/alarms/measurements"),
            ServiceRoute.of("POST", "/api/internal/gpu-agents/discover")
    );

    @Value("${manager.inbound.service-api-key:}")
    private String inboundServiceApiKey;

    @Value("${external-data.api-key:}")
    private String externalDataApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String apiKey = request.getHeader("X-Api-Key");
        String path = request.getRequestURI();

        if (isInternalServicePath(path)) {
            if (!matchesKey(inboundServiceApiKey, apiKey) || !isCollectorRoute(request.getMethod(), path)) {
                reject(response);
                return;
            }
            authenticateApiKey(COLLECTOR_PRINCIPAL);
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
            if (matchesKey(inboundServiceApiKey, apiKey) && isCollectorRoute(request.getMethod(), path)) {
                authenticateApiKey(COLLECTOR_PRINCIPAL);
                log.debug("API Key 인증 성공");
            } else {
                log.warn("유효하지 않은 API Key");
                reject(response);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    /** 사용자 JWT로는 호출할 수 없고 서비스 키가 반드시 필요한 내부 경로. */
    private static boolean isInternalServicePath(String path) {
        return path.equals("/api/internal/alarms/measurements") || path.equals("/api/internal/gpu-agents/discover");
    }

    static boolean isCollectorRoute(String method, String path) {
        return COLLECTOR_ROUTES.stream().anyMatch(route -> route.matches(method, path));
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

    static boolean matchesKey(String expected, String providedApiKey) {
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

    record ServiceRoute(String method, Pattern path) {
        static ServiceRoute of(String method, String pathRegex) {
            return new ServiceRoute(method, Pattern.compile(pathRegex));
        }

        boolean matches(String requestMethod, String requestPath) {
            return method.equals(requestMethod) && path.matcher(requestPath).matches();
        }
    }
}
