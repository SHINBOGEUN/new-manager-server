package net.vivans.dcim.module.device.domain.model;

import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.device.api.dto.DeviceModbusReadingResponse;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModelModbusPoint;
import net.vivans.dcim.module.devicemodel.domain.model.ModbusDataType;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DeviceModbusReadingTest {
    @Test
    void createAndUpdate_keepModbusParentAndResponseIds() {
        var protocol = mock(CommonCode.class);
        when(protocol.getCode()).thenReturn("modbus");
        var source = mock(Device.class);
        when(source.getId()).thenReturn(14);
        var commonEndpoint = mock(DeviceProtocolEndpoint.class);
        when(commonEndpoint.getProtocolType()).thenReturn(protocol);
        when(commonEndpoint.getDevice()).thenReturn(source);
        var config = DeviceEndpointModbus.create(commonEndpoint, null);
        ReflectionTestUtils.setField(config, "endpointId", 30);
        var point = mock(DeviceModelModbusPoint.class);
        when(point.getId()).thenReturn(1);
        when(point.isRequiresInstance()).thenReturn(true);
        when(point.getDataType()).thenReturn(ModbusDataType.FLOAT32);
        var target = mock(Device.class);
        when(target.getId()).thenReturn(20);

        var reading = DeviceModbusReading.create(config, point, 1, 11415, target, "TOTAL_WT", true);
        reading.update(point, 2, 11565, target, "POWER", false);

        assertThat(reading.getEndpointModbus()).isSameAs(config);
        var response = DeviceModbusReadingResponse.from(reading);
        assertThat(response.endpointId()).isEqualTo(30);
        assertThat(response.sourceDeviceId()).isEqualTo(14);
        assertThat(response.targetDeviceId()).isEqualTo(20);
        assertThat(response.pointId()).isEqualTo(1);
        assertThat(response.unitId()).isEqualTo(2);
        assertThat(response.address()).isEqualTo(11565);
        assertThat(response.pointName()).isEqualTo("POWER");
        assertThat(response.enabled()).isFalse();
    }

    @Test
    void createWithoutModbusConfig_isRejected() {
        assertThatThrownBy(() -> DeviceModbusReading.create(null, null, 0, 0, null, "TOTAL_WT", true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("endpoint is required");
    }
}
