package net.vivans.dcim.module.lora.domain.model;

import net.vivans.dcim.module.mqtt.domain.model.DeviceMqttIdentity;
import net.vivans.dcim.module.mqtt.domain.model.MqttIdentityType;

import net.vivans.dcim.module.device.domain.model.Device;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class DeviceLoraEndpointTest {

    @Test
    void create_setsFieldsAndNormalizesExternalId() {
        Device device = mock(Device.class);

        DeviceMqttIdentity endpoint = DeviceMqttIdentity.create(
                device, MqttIdentityType.DEV_EUI, "  24:E1:24:71:0C:12:34:56  ", true);

        assertThat(endpoint.getDevice()).isEqualTo(device);
        assertThat(endpoint.getIdentityType()).isEqualTo(MqttIdentityType.DEV_EUI);
        assertThat(endpoint.getExternalId()).isEqualTo("24:E1:24:71:0C:12:34:56");
        assertThat(endpoint.getNormalizedExternalId()).isEqualTo("24E124710C123456");
        assertThat(endpoint.isEnabled()).isTrue();
    }

    @Test
    void create_throwsWhenDeviceIsNull() {
        assertThatThrownBy(() -> DeviceMqttIdentity.create(null, MqttIdentityType.DEV_EUI, "abc", true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("device is required");
    }

    @Test
    void create_throwsWhenIdTypeIsNull() {
        Device device = mock(Device.class);
        assertThatThrownBy(() -> DeviceMqttIdentity.create(device, null, "abc", true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("idType is required");
    }

    @Test
    void create_throwsWhenExternalIdIsBlank() {
        Device device = mock(Device.class);
        assertThatThrownBy(() -> DeviceMqttIdentity.create(device, MqttIdentityType.DEVICE_NAME, "   ", true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("externalId is required");
    }

    @Test
    void update_changesExternalIdAndRecomputesNormalizedValue() {
        Device device = mock(Device.class);
        DeviceMqttIdentity endpoint = DeviceMqttIdentity.create(device, MqttIdentityType.DEVICE_NAME, "old-name", true);

        endpoint.update("dragino_new-name", false);

        assertThat(endpoint.getExternalId()).isEqualTo("dragino_new-name");
        assertThat(endpoint.getNormalizedExternalId()).isEqualTo("DRAGINONEWNAME");
        assertThat(endpoint.isEnabled()).isFalse();
    }
}
