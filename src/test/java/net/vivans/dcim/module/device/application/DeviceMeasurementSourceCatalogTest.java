package net.vivans.dcim.module.device.application;

import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.model.DeviceEndpointModbus;
import net.vivans.dcim.module.device.domain.model.DeviceModbusBitField;
import net.vivans.dcim.module.device.domain.model.DeviceModbusReading;
import net.vivans.dcim.module.device.domain.model.DeviceProtocolEndpoint;
import net.vivans.dcim.module.device.domain.repository.DeviceEndpointModbusRepository;
import net.vivans.dcim.module.device.domain.repository.DeviceModbusReadingRepository;
import net.vivans.dcim.module.device.domain.repository.DeviceProtocolEndpointRepository;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import net.vivans.dcim.module.device.infrastructure.persistence.DeviceModbusBitFieldRepository;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModel;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModelModbusPoint;
import net.vivans.dcim.module.devicemodel.domain.repository.DeviceModelModbusPointRepository;
import net.vivans.dcim.module.devicemodel.domain.repository.DeviceModelSnmpPointRepository;
import net.vivans.dcim.module.mqtt.domain.repository.DeviceModelMqttPointRepository;
import net.vivans.dcim.module.mqtt.domain.repository.DeviceMqttIdentityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceMeasurementSourceCatalogTest {
    @Mock DeviceRepository devices;
    @Mock DeviceModelSnmpPointRepository snmpPoints;
    @Mock DeviceModelModbusPointRepository modbusPoints;
    @Mock DeviceModbusReadingRepository readings;
    @Mock DeviceProtocolEndpointRepository endpoints;
    @Mock DeviceEndpointModbusRepository modbusEndpoints;
    @Mock DeviceModbusBitFieldRepository bitFields;
    @Mock DeviceModelMqttPointRepository mqttPoints;
    @Mock DeviceMqttIdentityRepository mqttIdentities;
    @InjectMocks DeviceMeasurementSourceCatalog catalog;

    @Test
    void mappedPowerAndBitFieldBelongToTargetDeviceNotPollingDevice() {
        Device target = mock(Device.class);
        DeviceModel targetModel = mock(DeviceModel.class);
        when(target.getId()).thenReturn(31);
        when(target.getName()).thenReturn("CHILLER");
        when(target.getDeviceModel()).thenReturn(targetModel);
        when(targetModel.getId()).thenReturn(10);
        when(devices.findAllEnabled()).thenReturn(List.of(target));
        when(endpoints.findAllByDeviceIdOrderByIdAsc(31)).thenReturn(List.of());

        DeviceModbusReading reading = mock(DeviceModbusReading.class);
        DeviceModelModbusPoint modelPoint = mock(DeviceModelModbusPoint.class);
        DeviceEndpointModbus modbus = mock(DeviceEndpointModbus.class);
        DeviceProtocolEndpoint endpoint = mock(DeviceProtocolEndpoint.class);
        Device source = mock(Device.class);
        when(readings.findAllByTargetDeviceIdOrderByIdAsc(31)).thenReturn(List.of(reading));
        when(reading.isEnabled()).thenReturn(true);
        when(reading.getPoint()).thenReturn(modelPoint);
        when(modelPoint.isEnabled()).thenReturn(true);
        when(modelPoint.getUnit()).thenReturn("W");
        when(reading.getPointName()).thenReturn("TOTAL_WT");
        when(reading.getId()).thenReturn(5);
        when(reading.getEndpointModbus()).thenReturn(modbus);
        when(modbus.getEndpoint()).thenReturn(endpoint);
        when(endpoint.isEnabled()).thenReturn(true);
        when(endpoint.getDevice()).thenReturn(source);
        when(source.getId()).thenReturn(17);

        DeviceModbusBitField bit = mock(DeviceModbusBitField.class);
        when(bitFields.findAllByReading_IdOrderByIdAsc(5)).thenReturn(List.of(bit));
        when(bit.getPointName()).thenReturn("VALVE_OPEN");

        List<DeviceMeasurementSourceCatalog.Source> result = catalog.availableSources();
        assertThat(result).extracting(DeviceMeasurementSourceCatalog.Source::pointName)
                .containsExactly("TOTAL_WT", "VALVE_OPEN");
        assertThat(result).allSatisfy(item -> {
            assertThat(item.deviceId()).isEqualTo(31);
            assertThat(item.sourceDeviceId()).isEqualTo(17);
            assertThat(item.protocol()).isEqualTo("modbus");
            assertThat(item.ambiguous()).isFalse();
        });
        assertThat(result.get(0).dataPointType()).isEqualTo("POWER");
        assertThat(result.get(1).dataPointType()).isEqualTo("STATUS");
    }
}
