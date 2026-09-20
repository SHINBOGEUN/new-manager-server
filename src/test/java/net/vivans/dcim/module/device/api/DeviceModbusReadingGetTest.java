package net.vivans.dcim.module.device.api;

import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.device.application.DeviceModbusReadingQueryService;
import net.vivans.dcim.module.device.domain.model.*;
import net.vivans.dcim.module.device.domain.repository.*;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModelModbusPoint;
import net.vivans.dcim.module.devicemodel.domain.model.ModbusDataType;
import net.vivans.dcim.module.devicemodel.domain.repository.DeviceModelModbusPointRepository;
import net.vivans.dcim.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 실제 서비스와 컨트롤러를 연결하고 저장소만 대체합니다. 실제 DB에는 접속하지 않습니다. */
class DeviceModbusReadingGetTest {
    private static final String URL = "/api/manager/devices/14/endpoints/30/modbus/readings";
    private final DeviceRepository devices = mock(DeviceRepository.class);
    private final DeviceProtocolEndpointRepository endpoints = mock(DeviceProtocolEndpointRepository.class);
    private final DeviceEndpointModbusRepository configs = mock(DeviceEndpointModbusRepository.class);
    private final DeviceModbusReadingRepository readings = mock(DeviceModbusReadingRepository.class);
    private final CommonCode protocol = mock(CommonCode.class);
    private DeviceEndpointModbus config;
    private Device device;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        device = mock(Device.class);
        when(device.getId()).thenReturn(14);
        when(devices.findById(14)).thenReturn(Optional.of(device));
        var endpoint = mock(DeviceProtocolEndpoint.class);
        when(endpoint.getProtocolType()).thenReturn(protocol);
        when(endpoint.getDevice()).thenReturn(device);
        when(protocol.getCode()).thenReturn("modbus");
        when(endpoints.findByIdAndDeviceId(30, 14)).thenReturn(Optional.of(endpoint));
        config = DeviceEndpointModbus.create(endpoint, null);
        ReflectionTestUtils.setField(config, "endpointId", 30);
        when(configs.findByEndpointId(30)).thenReturn(Optional.of(config));
        var service = new DeviceModbusReadingQueryService(devices, endpoints, configs,
                mock(DeviceModelModbusPointRepository.class), readings);
        mvc = MockMvcBuilders.standaloneSetup(new DeviceModbusReadingController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void listIncludesDisabledReadingsAndPreservesRepositoryOrder() throws Exception {
        var results = List.of(reading(1, true), reading(2, false));
        when(readings.findAllByEndpointIdOrderByIdAsc(30))
                .thenReturn(results);
        mvc.perform(get(URL)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[1].id").value(2))
                .andExpect(jsonPath("$.data[1].enabled").value(false))
                .andExpect(jsonPath("$.data[0].endpointId").value(30))
                .andExpect(jsonPath("$.data[0].sourceDeviceId").value(14));
    }

    @Test
    void emptyListReturns200() throws Exception {
        when(readings.findAllByEndpointIdOrderByIdAsc(30)).thenReturn(List.of());
        mvc.perform(get(URL)).andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void getSingleReading() throws Exception {
        var result = reading(1, false);
        when(readings.findByIdAndEndpointId(1, 30)).thenReturn(Optional.of(result));
        mvc.perform(get(URL + "/1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.enabled").value(false));
    }

    @Test
    void absentOrOtherEndpointReadingReturns404() throws Exception {
        when(readings.findByIdAndEndpointId(99, 30)).thenReturn(Optional.empty());
        mvc.perform(get(URL + "/99")).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/1"})
    void missingDeviceReturns404(String suffix) throws Exception {
        when(devices.findById(14)).thenReturn(Optional.empty());
        mvc.perform(get(URL + suffix)).andExpect(status().isNotFound());
        verifyNoInteractions(readings);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/1"})
    void missingOrOtherDeviceEndpointReturns404(String suffix) throws Exception {
        when(endpoints.findByIdAndDeviceId(30, 14)).thenReturn(Optional.empty());
        mvc.perform(get(URL + suffix)).andExpect(status().isNotFound());
        verifyNoInteractions(readings);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/1"})
    void missingModbusConfigReturns404(String suffix) throws Exception {
        when(configs.findByEndpointId(30)).thenReturn(Optional.empty());
        mvc.perform(get(URL + suffix)).andExpect(status().isNotFound());
        verifyNoInteractions(readings);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/1"})
    void nonModbusEndpointReturns400(String suffix) throws Exception {
        when(protocol.getCode()).thenReturn("snmp");
        mvc.perform(get(URL + suffix)).andExpect(status().isBadRequest());
        verifyNoInteractions(readings);
    }

    private DeviceModbusReading reading(int id, boolean enabled) {
        var point = mock(DeviceModelModbusPoint.class);
        when(point.getId()).thenReturn(1);
        when(point.isRequiresInstance()).thenReturn(true);
        when(point.getDataType()).thenReturn(ModbusDataType.FLOAT32);
        var reading = DeviceModbusReading.create(config, point, 0, 11265, device, "POWER_" + id, enabled);
        ReflectionTestUtils.setField(reading, "id", id);
        return reading;
    }
}
