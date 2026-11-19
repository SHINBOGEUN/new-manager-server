package net.vivans.dcim.module.device.application;

import net.vivans.dcim.module.collectortask.application.CollectionReconciler;
import net.vivans.dcim.module.device.api.dto.DeviceModbusBitFieldRequest;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.model.DeviceEndpointModbus;
import net.vivans.dcim.module.device.domain.model.DeviceModbusReading;
import net.vivans.dcim.module.device.domain.model.DeviceProtocolEndpoint;
import net.vivans.dcim.module.device.domain.repository.DeviceModbusReadingRepository;
import net.vivans.dcim.module.device.infrastructure.persistence.DeviceModbusBitFieldRepository;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModel;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModelModbusPoint;
import net.vivans.dcim.module.devicemodel.domain.model.ModbusDataType;
import net.vivans.dcim.module.devicemodel.domain.model.ModbusRegisterType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeviceModbusBitFieldServiceTest {

    @Test
    void replacingBitFieldsRegeneratesSourceModelJobs() {
        DeviceModbusReadingRepository readings = mock(DeviceModbusReadingRepository.class);
        DeviceModbusBitFieldRepository fields = mock(DeviceModbusBitFieldRepository.class);
        CollectionReconciler sync = mock(CollectionReconciler.class);
        DeviceModbusReading reading = mock(DeviceModbusReading.class);
        DeviceModelModbusPoint point = mock(DeviceModelModbusPoint.class);
        DeviceEndpointModbus modbusEndpoint = mock(DeviceEndpointModbus.class);
        DeviceProtocolEndpoint endpoint = mock(DeviceProtocolEndpoint.class);
        Device source = mock(Device.class);
        Device target = mock(Device.class);
        DeviceModel model = mock(DeviceModel.class);
        when(readings.findByIdAndEndpointId(11, 2)).thenReturn(Optional.of(reading));
        when(reading.getId()).thenReturn(11);
        when(reading.getPoint()).thenReturn(point);
        when(point.getRegisterType()).thenReturn(ModbusRegisterType.HOLDING);
        when(point.getDataType()).thenReturn(ModbusDataType.UINT16);
        when(point.getScale()).thenReturn(1.0);
        when(reading.getPointName()).thenReturn("VALVE_STATUS_RAW");
        when(reading.getEndpointModbus()).thenReturn(modbusEndpoint);
        when(modbusEndpoint.getEndpoint()).thenReturn(endpoint);
        when(endpoint.getDevice()).thenReturn(source);
        when(source.getId()).thenReturn(32);
        when(source.getDeviceModel()).thenReturn(model);
        when(model.getId()).thenReturn(5);
        when(reading.getTargetDevice()).thenReturn(target);
        when(target.getId()).thenReturn(32);
        when(readings.findAllByTargetDeviceIdOrderByIdAsc(32)).thenReturn(List.of(reading));
        when(fields.findAllByReading_IdOrderByIdAsc(11)).thenReturn(List.of());
        DeviceModbusBitFieldService service = new DeviceModbusBitFieldService(readings, fields, sync);

        service.replace(32, 2, 11, List.of(new DeviceModbusBitFieldRequest(
                "VALVE", 0, 2, Map.of("2", 1L), null)));

        verify(fields).saveAll(anyList());
        verify(sync).reconcileModel(5);
    }
}
