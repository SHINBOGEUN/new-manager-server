package net.vivans.dcim.shared.security;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.shared.exception.JwtAccessDeniedHandler;
import net.vivans.dcim.shared.exception.JwtAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${security.auth-enabled:false}")
    private boolean authEnabled;

    /**
     * 브라우저 교차 출처 호출을 허용할 Origin 목록(쉼표 구분, 패턴 가능). 개발 기본값은 *, 운영(prod)은 기본으로 비워 같은 출처만 허용한다.
     * React 개발 서버 등 다른 주소의 화면을 쓰면 CORS_ALLOWED_ORIGINS에 그 주소를 넣는다.
     */
    @Value("${security.cors.allowed-origins:*}")
    private String allowedOrigins;

    private final ApiKeyAuthFilter apiKeyAuthFilter;

    /** CORS와 WebSocket 핸드셰이크가 같은 Origin 목록을 쓰도록 공용으로 제공한다. */
    public static List<String> parseOrigins(String value) {
        if (value == null) return List.of();
        return java.util.Arrays.stream(value.split(",")).map(String::strip).filter(origin -> !origin.isEmpty()).toList();
    }
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                );

        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.exceptionHandling(ex -> ex
                .accessDeniedHandler(jwtAccessDeniedHandler)
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
        );

        if (authEnabled) {
            http.authorizeHttpRequests(request -> request
                .requestMatchers("/api/manager/auth/login", "/api/manager/auth/register", "/api/manager/auth/refresh", "/api/manager/auth/logout", "/api/manager/auth/validate", "/api/manager/auth/registration").permitAll()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/api-docs/**", "/swagger-resources/**", "/webjars/**").permitAll()
                .requestMatchers("/", "/index.html", "/analysis.html", "/live-test.html", "/mqtt-monitor.html", "/widget-dashboard.html", "/ops-console.html", "/page-scene-3d.js", "/floor-placement.js", "/inrow-placement.js", "/scene-group-lock.js", "/asset-model-preview.js", "/vendor/three/**", "/vendor/chartjs/**", "/vendor/mqtt/**", "/vendor/pretendard/**").permitAll()
                .requestMatchers("/scene-assets/showroom/**", "/showroom-models.js", "/rack-widget-layout.js", "/rack-widget-chart.js", "/rack-door-attachment.js", "/rack-scene-display.js", "/ops-navigation.js", "/ops-workspace.css").permitAll()
                .requestMatchers("/favicon.svg", "/favicon.ico").permitAll()
                .requestMatchers("/ws/mqtt-monitor").permitAll()
                .requestMatchers("/ws/alarms").permitAll()
                .anyRequest()
                .authenticated()
            );
            http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        } else {
            http.authorizeHttpRequests(request -> request
                .requestMatchers("/api/manager/auth/**").denyAll()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/api-docs/**", "/swagger-resources/**", "/webjars/**").permitAll()
                .anyRequest().permitAll()
            );
        }

        http.addFilterAfter(apiKeyAuthFilter, SecurityContextHolderFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(parseOrigins(allowedOrigins));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("*"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
