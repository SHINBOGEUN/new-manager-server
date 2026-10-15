package net.vivans.dcim.module.mqtt.domain.model;

import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

class DeviceModelMqttPointTest {
    @Test
    void legacyLoraPointDefaultsToRootDeviceScope() {
        DeviceModelMqttPoint point = point("object.TempC_SHT");
        assertThat(point.getComponentScope()).isEqualTo(MqttPointScope.DEVICE);
        assertThat(point.getPayloadField()).isEqualTo("object.TempC_SHT");
        assertThat(point.getComponentPath()).isEmpty();
        assertThat(point.getComponentIndexField()).isNull();
    }

    @Test
    void oneDefinitionDescribesAnyNumberOfGpuComponents() {
        DeviceModelMqttPoint point = point("gpu_temperature");
        point.configureComponent("GPU", "gpus", "gpu_index", "gpu_uuid");
        assertThat(point.getComponentScope()).isEqualTo(MqttPointScope.COMPONENT);
        assertThat(point.getComponentPath()).isEqualTo("gpus");
        assertThat(point.getComponentIndexField()).isEqualTo("gpu_index");
        assertThat(point.getComponentUuidField()).isEqualTo("gpu_uuid");
        assertThatThrownBy(() -> point.configureComponent("GPU", "gpus", " ", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(point.getComponentIndexField()).isEqualTo("gpu_index");
    }

    private DeviceModelMqttPoint point(String path) {
        return DeviceModelMqttPoint.create(mock(DeviceModel.class), path, "TEMPERATURE",
                mock(CommonCode.class), null, 1.0, null, true);
    }
}
