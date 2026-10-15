package net.vivans.dcim.module.mqtt.application;

import java.util.List;
import java.util.Optional;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.vivans.dcim.module.common.domain.model.*;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModel;
import net.vivans.dcim.module.mqtt.domain.model.*;
import net.vivans.dcim.module.mqtt.domain.repository.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class MqttLookupServiceTest {
    private final DeviceMqttIdentityRepository identities = mock(DeviceMqttIdentityRepository.class);
    private final DeviceModelMqttPointRepository points = mock(DeviceModelMqttPointRepository.class);
    private final MqttLookupService service = new MqttLookupService(identities, points);

    @Test
    void resolveNormalizesMacAndReturnsStableDeviceId() {
        var identity = identity(MqttIdentityType.MAC, "9c:6b:00:9b:4b:d7", true, true);
        when(identities.findByIdentityTypeAndNormalizedExternalIdAndEnabledTrue(MqttIdentityType.MAC, "9C6B009B4BD7"))
                .thenReturn(Optional.of(identity));
        var response = service.resolve(MqttIdentityType.MAC, "9c:6b:00:9b:4b:d7").orElseThrow();
        assertThat(response.deviceId()).isEqualTo(40);
        assertThat(response.identityType()).isEqualTo(MqttIdentityType.MAC);
        assertThat(response.normalizedExternalId()).isEqualTo("9C6B009B4BD7");
    }

    @Test
    void resolveMissingOrDisabledDeviceReturnsEmpty() {
        when(identities.findByIdentityTypeAndNormalizedExternalIdAndEnabledTrue(MqttIdentityType.SERIAL, "unknown"))
                .thenReturn(Optional.empty());
        assertThat(service.resolve(MqttIdentityType.SERIAL, "unknown")).isEmpty();
        var disabled = identity(MqttIdentityType.MAC, "aabbccddeeff", true, false);
        when(identities.findByIdentityTypeAndNormalizedExternalIdAndEnabledTrue(MqttIdentityType.MAC, "AABBCCDDEEFF"))
                .thenReturn(Optional.of(disabled));
        assertThat(service.resolve(MqttIdentityType.MAC, "aabbccddeeff")).isEmpty();
    }

    @Test
    void bulkSupportsAllIdentityTypesAndExcludesDisabledEntries() {
        var entries = List.of(
                identity(MqttIdentityType.MAC, "aabbccddeeff", true, true),
                identity(MqttIdentityType.DEV_EUI, "24E124710C123456", true, true),
                identity(MqttIdentityType.DEVICE_NAME, "sensor-1", true, true),
                identity(MqttIdentityType.UUID, "123e4567-e89b-12d3-a456-426614174000", true, true),
                identity(MqttIdentityType.SERIAL, "Serial-1", true, true),
                identity(MqttIdentityType.SERIAL, "disabled-identity", false, true),
                identity(MqttIdentityType.SERIAL, "disabled-device", true, false));
        when(identities.findAllOrderByIdAsc()).thenReturn(entries);
        assertThat(service.getAllEnabledIdentities()).extracting(r -> r.identityType())
                .containsExactly(MqttIdentityType.MAC, MqttIdentityType.DEV_EUI, MqttIdentityType.DEVICE_NAME,
                        MqttIdentityType.UUID, MqttIdentityType.SERIAL);
    }

    @Test
    void invalidMacDoesNotQueryRepository() {
        assertThatThrownBy(() -> service.resolve(MqttIdentityType.MAC, "invalid"))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(identities);
    }

    @Test
    void mappingsIncludeDeviceAndComponentPathsWithoutChangingLoraPath() {
        var model = model();
        var type = CommonCode.createCommonCode(CodeGroup.createCodeGroup("DATA_POINT_TYPE", "Point"), "TEMP", "Temp", 1);
        var lora = DeviceModelMqttPoint.create(model, "object.TempC_SHT", "TEMPERATURE", type, null, 1.0, null, true);
        var gpu = DeviceModelMqttPoint.create(model, "gpu_temperature", "GPU_TEMP", type, null, 1.0, null, true);
        gpu.configureComponent("GPU", "gpus", "gpu_index", "gpu_uuid");
        when(points.findAllEnabled()).thenReturn(List.of(lora, gpu));
        var mappings = service.getAllEnabledModelMappings();
        assertThat(mappings).hasSize(2);
        assertThat(mappings.get(0).payloadField()).isEqualTo("object.TempC_SHT");
        assertThat(mappings.get(0).componentScope()).isEqualTo(MqttPointScope.DEVICE);
        var json = new ObjectMapper().valueToTree(mappings.get(1));
        assertThat(json.get("componentScope").asText()).isEqualTo("COMPONENT");
        assertThat(json.get("componentPath").asText()).isEqualTo("gpus");
        assertThat(json.get("componentIndexField").asText()).isEqualTo("gpu_index");
        assertThat(json.get("componentUuidField").asText()).isEqualTo("gpu_uuid");
    }

    private DeviceMqttIdentity identity(MqttIdentityType type, String value, boolean enabled, boolean deviceEnabled) {
        var device = mock(Device.class);
        when(device.getId()).thenReturn(40);
        when(device.getName()).thenReturn("GPU server 1");
        when(device.isEnabled()).thenReturn(deviceEnabled);
        when(device.getDeviceModel()).thenReturn(model());
        return DeviceMqttIdentity.create(device, type, value, enabled);
    }

    private DeviceModel model() {
        var type = CommonCode.createCommonCode(CodeGroup.createCodeGroup("MODEL_TYPE", "Model"), "GPU_SERVER", "GPU", 1);
        return DeviceModel.create("GPU", "NVIDIA", type, null);
    }
}
