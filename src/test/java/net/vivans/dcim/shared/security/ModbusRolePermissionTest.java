package net.vivans.dcim.shared.security;

import net.vivans.dcim.module.device.api.DeviceEndpointModbusController;
import net.vivans.dcim.module.device.api.DeviceModbusReadingController;
import net.vivans.dcim.module.device.application.DeviceEndpointModbusQueryService;
import net.vivans.dcim.module.device.application.DeviceModbusReadingQueryService;
import net.vivans.dcim.module.devicemodel.api.DeviceModelModbusPointController;
import net.vivans.dcim.module.devicemodel.application.DeviceModelModbusPointQueryService;
import net.vivans.dcim.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** DB/JWT 발급 없이 실제 Spring 메서드 보안 프록시와 HTTP 응답을 검증합니다. */
@SpringJUnitConfig(ModbusRolePermissionTest.Config.class)
class ModbusRolePermissionTest {
    private static final String POINT = "/api/manager/device-models/4/protocols/4/modbus-points";
    private static final String ENDPOINT = "/api/manager/devices/14/endpoints/14/modbus";
    private static final String READING = ENDPOINT + "/readings";
    private static final String POINT_BODY = """
            {"name":"TOTAL_WT","registerType":"HOLDING","dataType":"FLOAT32",
             "byteOrder":"CDAB","requiresInstance":true,"scale":1000,"unit":"W","enabled":true}
            """;
    private static final String READING_BODY = """
            {"pointId":1,"unitId":0,"address":11265,"targetDeviceId":14,"pointName":"TOTAL_WT","enabled":true}
            """;

    @Autowired DeviceModelModbusPointController pointController;
    @Autowired DeviceEndpointModbusController endpointController;
    @Autowired DeviceModbusReadingController readingController;
    @Autowired DeviceModelModbusPointQueryService points;
    @Autowired DeviceEndpointModbusQueryService endpoints;
    @Autowired DeviceModbusReadingQueryService readings;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        reset(points, endpoints, readings);
        mvc = MockMvcBuilders.standaloneSetup(pointController, endpointController, readingController)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    static Stream<Arguments> writes() {
        return Stream.of("ADMIN", "USER", "GUEST").flatMap(role -> Stream.of(
                Arguments.of(role, "POST", POINT, POINT_BODY),
                Arguments.of(role, "PUT", POINT + "/1", POINT_BODY),
                Arguments.of(role, "DELETE", POINT + "/1", ""),
                Arguments.of(role, "POST", ENDPOINT, "{\"unitId\":1}"),
                Arguments.of(role, "PUT", ENDPOINT, "{\"unitId\":1}"),
                Arguments.of(role, "DELETE", ENDPOINT, ""),
                Arguments.of(role, "POST", READING, READING_BODY),
                Arguments.of(role, "PUT", READING + "/1", READING_BODY),
                Arguments.of(role, "DELETE", READING + "/1", "")
        ));
    }

    @ParameterizedTest(name = "{0} {1} {2}")
    @MethodSource("writes")
    void onlyAdminCanReachMutationService(String role, String method, String path, String body) throws Exception {
        authenticate(role);
        mvc.perform(request(HttpMethod.valueOf(method), path)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().is(role.equals("ADMIN") ? 200 : 403));
        if (role.equals("ADMIN")) {
            assertThat(List.of(points, endpoints, readings).stream()
                    .mapToInt(service -> mockingDetails(service).getInvocations().size()).sum()).isEqualTo(1);
        } else {
            verifyNoInteractions(points, endpoints, readings);
        }
    }

    static Stream<Arguments> reads() {
        return Stream.of("ADMIN", "USER", "GUEST").flatMap(role ->
                Stream.of(POINT, POINT + "/1", ENDPOINT, READING, READING + "/1")
                        .map(path -> Arguments.of(role, path)));
    }

    @ParameterizedTest(name = "{0} GET {1}")
    @MethodSource("reads")
    void authenticatedRolesKeepReadAccess(String role, String path) throws Exception {
        authenticate(role);
        mvc.perform(request(HttpMethod.GET, path)).andExpect(status().isOk());
    }

    private void authenticate(String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("test", "unused",
                        AuthorityUtils.createAuthorityList("ROLE_" + role)));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    @Import({DeviceModelModbusPointController.class, DeviceEndpointModbusController.class,
            DeviceModbusReadingController.class})
    static class Config {
        @Bean DeviceModelModbusPointQueryService points() { return mock(DeviceModelModbusPointQueryService.class); }
        @Bean DeviceEndpointModbusQueryService endpoints() { return mock(DeviceEndpointModbusQueryService.class); }
        @Bean DeviceModbusReadingQueryService readings() { return mock(DeviceModbusReadingQueryService.class); }
    }
}
