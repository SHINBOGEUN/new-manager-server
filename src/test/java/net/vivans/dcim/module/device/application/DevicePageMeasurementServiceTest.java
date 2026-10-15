package net.vivans.dcim.module.device.application;

import jakarta.persistence.EntityNotFoundException;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.common.domain.repository.CommonCodeRepository;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.model.DeviceModbusBitField;
import net.vivans.dcim.module.device.domain.model.DeviceModbusReading;
import net.vivans.dcim.module.device.domain.model.DevicePageDevice;
import net.vivans.dcim.module.device.domain.repository.DeviceModbusReadingRepository;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import net.vivans.dcim.module.device.infrastructure.persistence.DeviceModbusBitFieldRepository;
import net.vivans.dcim.module.device.infrastructure.persistence.DevicePageDeviceRepository;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModel;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModelModbusPoint;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModelProtocol;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModelSnmpPoint;
import net.vivans.dcim.module.devicemodel.domain.repository.DeviceModelModbusPointRepository;
import net.vivans.dcim.module.devicemodel.domain.repository.DeviceModelSnmpPointRepository;
import net.vivans.dcim.module.location.domain.model.LocationNode;
import net.vivans.dcim.module.mqtt.domain.model.DeviceModelMqttPoint;
import net.vivans.dcim.module.mqtt.domain.model.MqttPointScope;
import net.vivans.dcim.module.mqtt.domain.repository.DeviceModelMqttPointRepository;
import net.vivans.dcim.module.query.domain.LastPoint;
import net.vivans.dcim.module.query.domain.PointQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DevicePageMeasurementServiceTest {
    @Mock CommonCodeRepository commonCodeRepository;
    @Mock DeviceRepository deviceRepository;
    @Mock DevicePageDeviceRepository pageDeviceRepository;
    @Mock DeviceModelSnmpPointRepository snmpPointRepository;
    @Mock DeviceModelModbusPointRepository modbusPointRepository;
    @Mock DeviceModbusReadingRepository modbusReadingRepository;
    @Mock DeviceModbusBitFieldRepository bitFieldRepository;
    @Mock DeviceModelMqttPointRepository mqttPointRepository;
    @Mock PointQuery pointQuery;
    @InjectMocks DevicePageMeasurementService service;

    @Test
    void returnsEveryConfiguredPointIncludingMissingValuesAcrossProtocols() {
        CommonCode page = page();
        Device snmpDevice = device(1, 11, "PDU");
        Device modbusDevice = device(2, 22, "PLC");
        Device loraDevice = device(3, 33, "Sensor");
        when(pageDeviceRepository.findAllByPageCode_IdOrderByIdAsc(10))
                .thenReturn(List.of(mapping(page, snmpDevice), mapping(page, modbusDevice), mapping(page, loraDevice)));

        DeviceModelProtocol snmpProtocol = mock(DeviceModelProtocol.class);
        DeviceModel snmpModel = snmpDevice.getDeviceModel();
        when(snmpProtocol.getDeviceModel()).thenReturn(snmpModel);
        DeviceModelSnmpPoint snmpPoint = mock(DeviceModelSnmpPoint.class);
        when(snmpPoint.getModelProtocol()).thenReturn(snmpProtocol);
        when(snmpPoint.getName()).thenReturn("TEMP");
        when(snmpPoint.getUnit()).thenReturn("°C");
        when(snmpPointRepository.findAllEnabledByDeviceModelIds(anyCollection())).thenReturn(List.of(snmpPoint));

        DeviceModelModbusPoint modbusPoint = mock(DeviceModelModbusPoint.class);
        when(modbusPoint.isEnabled()).thenReturn(true);
        when(modbusPoint.getUnit()).thenReturn(null);
        DeviceModbusReading reading = mock(DeviceModbusReading.class);
        when(reading.getId()).thenReturn(7);
        when(reading.isEnabled()).thenReturn(true);
        when(reading.getPoint()).thenReturn(modbusPoint);
        when(reading.getPointName()).thenReturn("VALVE_RAW");
        when(reading.getTargetDevice()).thenReturn(modbusDevice);
        when(modbusReadingRepository.findAllByTargetDeviceIds(anyCollection())).thenReturn(List.of(reading));
        DeviceModbusBitField bit = mock(DeviceModbusBitField.class);
        when(bit.getReading()).thenReturn(reading);
        when(bit.getPointName()).thenReturn("VALVE_1");
        when(bitFieldRepository.findAllByReading_IdInOrderByIdAsc(List.of(7))).thenReturn(List.of(bit));

        DeviceModelMqttPoint loraPoint = mock(DeviceModelMqttPoint.class);
        when(loraPoint.getComponentScope()).thenReturn(MqttPointScope.DEVICE);
        DeviceModel loraModel = loraDevice.getDeviceModel();
        when(loraPoint.getDeviceModel()).thenReturn(loraModel);
        when(loraPoint.getPointName()).thenReturn("HUM");
        when(loraPoint.getUnit()).thenReturn("%");
        CommonCode humidityType = mock(CommonCode.class);
        when(humidityType.getCode()).thenReturn("HUMIDITY");
        when(loraPoint.getDataPointType()).thenReturn(humidityType);
        DeviceModelMqttPoint gpuPoint = mock(DeviceModelMqttPoint.class);
        when(gpuPoint.getComponentScope()).thenReturn(MqttPointScope.COMPONENT);
        when(mqttPointRepository.findAllEnabledByDeviceModelIdIn(anyCollection())).thenReturn(List.of(loraPoint, gpuPoint));

        Instant time = Instant.parse("2026-10-03T01:00:00Z");
        when(pointQuery.findLast(eq(List.of(1)), eq(List.of("TEMP")), eq(Duration.ofHours(168)), eq("snmp")))
                .thenReturn(List.of(new LastPoint(1, "TEMP", 23.5, time)));
        when(pointQuery.findLast(eq(List.of(2)), any(), eq(Duration.ofHours(168)), eq("modbus")))
                .thenReturn(List.of(new LastPoint(2, "VALVE_1", 1, time)));
        when(pointQuery.findLast(eq(List.of(3)), eq(List.of("HUM")), eq(Duration.ofHours(168)), eq("mqtt")))
                .thenReturn(List.of(new LastPoint(3, "HUM", 48.2, time)));

        var response = service.getMeasurements("COOLING", 168);

        assertThat(response.pageCode()).isEqualTo("COOLING");
        assertThat(response.devices()).hasSize(3);
        assertThat(response.devices().get(0).points().get(0).value()).isEqualTo(23.5);
        assertThat(response.devices().get(0).points().get(0).protocol()).isEqualTo("snmp");
        assertThat(response.devices().get(1).points()).extracting("pointName")
                .containsExactly("VALVE_RAW", "VALVE_1");
        assertThat(response.devices().get(1).points().get(0).value()).isNull();
        assertThat(response.devices().get(1).points().get(1).value()).isEqualTo(1.0);
        assertThat(response.devices().get(2).points().get(0).protocol()).isEqualTo("mqtt");
        assertThat(response.devices().get(2).points().get(0).value()).isEqualTo(48.2);
    }

    @Test
    void replacesOnlyPageMembershipAndRejectsUnknownDevices() {
        CommonCode page = page();
        Device original = device(1, 11, "PDU");
        Device replacement = device(2, 22, "PLC");
        when(pageDeviceRepository.findAllByPageCode_IdOrderByIdAsc(10)).thenReturn(List.of(mapping(page, original)));
        when(deviceRepository.findById(2)).thenReturn(Optional.of(replacement));

        assertThat(service.replaceDevices("COOLING", List.of(2, 2))).containsExactly(2);

        ArgumentCaptor<List<DevicePageDevice>> deleted = ArgumentCaptor.forClass(List.class);
        verify(pageDeviceRepository).deleteAll(deleted.capture());
        assertThat(deleted.getValue()).hasSize(1);
        ArgumentCaptor<DevicePageDevice> saved = ArgumentCaptor.forClass(DevicePageDevice.class);
        verify(pageDeviceRepository).save(saved.capture());
        assertThat(saved.getValue().getDevice()).isSameAs(replacement);

        assertThatThrownBy(() -> service.replaceDevices("COOLING", List.of(999)))
                .isInstanceOf(EntityNotFoundException.class).hasMessage("Device not found: 999");
    }

    private CommonCode page() {
        CommonCode page = mock(CommonCode.class);
        when(page.getId()).thenReturn(10);
        lenient().when(page.getCode()).thenReturn("COOLING");
        lenient().when(page.getName()).thenReturn("Cooling");
        when(commonCodeRepository.findByCodeGroupGroupKeyAndCode("DEVICE_PAGE", "COOLING"))
                .thenReturn(Optional.of(page));
        return page;
    }

    private Device device(int id, int modelId, String name) {
        Device device = mock(Device.class);
        DeviceModel model = mock(DeviceModel.class);
        LocationNode location = mock(LocationNode.class);
        lenient().when(device.getId()).thenReturn(id);
        lenient().when(device.getDeviceModel()).thenReturn(model);
        lenient().when(model.getId()).thenReturn(modelId);
        lenient().when(device.getName()).thenReturn(name);
        lenient().when(model.getName()).thenReturn(name + " model");
        lenient().when(device.getLocationNode()).thenReturn(location);
        lenient().when(location.getCode()).thenReturn("ROOM");
        lenient().when(location.getName()).thenReturn("Room");
        return device;
    }

    private DevicePageDevice mapping(CommonCode page, Device device) {
        return DevicePageDevice.create(page, device);
    }
}
