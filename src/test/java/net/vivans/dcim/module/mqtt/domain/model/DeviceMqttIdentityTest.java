package net.vivans.dcim.module.mqtt.domain.model;

import net.vivans.dcim.module.device.domain.model.Device;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DeviceMqttIdentityTest {
    @Test
    void macChangeKeepsIdentityAndDeviceIds() {
        Device device = mock(Device.class);
        when(device.getId()).thenReturn(40);
        DeviceMqttIdentity identity = DeviceMqttIdentity.create(device, MqttIdentityType.MAC, "9c:6b:00:9b:4b:d7", true);
        ReflectionTestUtils.setField(identity, "id", 8);

        identity.update("AA-BB-CC-DD-EE-FF", true);

        assertThat(identity.getId()).isEqualTo(8);
        assertThat(identity.getDevice().getId()).isEqualTo(40);
        assertThat(identity.getNormalizedExternalId()).isEqualTo("AABBCCDDEEFF");
        assertThat(identity.getIdentityType()).isEqualTo(MqttIdentityType.MAC);
    }

    @Test
    void invalidMacDoesNotMutateExistingIdentity() {
        DeviceMqttIdentity identity = DeviceMqttIdentity.create(mock(Device.class), MqttIdentityType.MAC,
                "9c6b009b4bd7", true);
        assertThatThrownBy(() -> identity.update("not-a-mac", false)).isInstanceOf(IllegalArgumentException.class);
        assertThat(identity.getNormalizedExternalId()).isEqualTo("9C6B009B4BD7");
        assertThat(identity.getExternalId()).isEqualTo("9c6b009b4bd7");
        assertThat(identity.isEnabled()).isTrue();
    }

    @Test
    void serialPreservesCaseAndSeparatorsWhileUuidUsesCanonicalFormat() {
        assertThat(MqttExternalIdNormalizer.normalize(MqttIdentityType.SERIAL, " Ab-12_3 ")).isEqualTo("Ab-12_3");
        assertThat(MqttExternalIdNormalizer.normalize(MqttIdentityType.UUID,
                "9B006B9C-464B-0000-0000-000000000000")).isEqualTo("9b006b9c-464b-0000-0000-000000000000");
    }

    @Test
    void loraNormalizationKeepsExistingMatchingRules() {
        assertThat(MqttExternalIdNormalizer.normalize(MqttIdentityType.DEV_EUI, " 24:E1:24:71:0C:12:34:56 "))
                .isEqualTo("24E124710C123456");
        assertThat(MqttExternalIdNormalizer.normalize(MqttIdentityType.DEVICE_NAME, "dragino_new-name"))
                .isEqualTo("DRAGINONEWNAME");
        assertThatThrownBy(() -> MqttExternalIdNormalizer.normalize(MqttIdentityType.DEVICE_NAME, "---"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
