package net.vivans.dcim.module.lora.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.vivans.dcim.module.common.domain.model.CodeGroup;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModel;
import net.vivans.dcim.module.mqtt.domain.model.*;
import net.vivans.dcim.module.mqtt.domain.repository.*;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoraLookupServiceTest {
    private final DeviceMqttIdentityRepository identities = mock(DeviceMqttIdentityRepository.class);
    private final DeviceModelMqttPointRepository points = mock(DeviceModelMqttPointRepository.class);
    private final LoraLookupService service = new LoraLookupService(identities, points);

    @Test
    void bulkKeepsLegacyJsonAndDoesNotExposeGpuConfiguration() throws Exception {
        Device lora = mock(Device.class);
        DeviceModel loraModel = model("LORA_SENSOR");
        when(lora.getDeviceModel()).thenReturn(loraModel);
        var eui = DeviceMqttIdentity.create(lora, MqttIdentityType.DEV_EUI, "24E124710C123456", true);
        var mac = DeviceMqttIdentity.create(mock(Device.class), MqttIdentityType.MAC, "aabbccddeeff", true);
        when(identities.findAllOrderByIdAsc()).thenReturn(List.of(eui, mac));

        var temperature = point(loraModel, "object.TempC_SHT");
        var gpu = point(model("GPU_SERVER"), "gpu_temperature");
        gpu.configureComponent("GPU", "gpus", "gpu_index", "gpu_uuid");
        when(points.findAllEnabled()).thenReturn(List.of(temperature, gpu));

        assertThat(service.getAllEnabledEndpoints()).hasSize(1);
        assertThat(service.getAllEnabledModelMappings()).hasSize(1);
        var mapper = new ObjectMapper();
        var endpointJson = mapper.valueToTree(service.getAllEnabledEndpoints().get(0));
        assertThat(endpointJson.get("idType").asText()).isEqualTo("DEV_EUI");
        var pointJson = mapper.valueToTree(service.getAllEnabledModelMappings().get(0));
        assertThat(pointJson.get("payloadField").asText()).isEqualTo("object.TempC_SHT");
        assertThat(pointJson.has("componentScope")).isFalse();
    }

    @Test
    void loraResolveRejectsMacAndNeverLooksItUp() {
        assertThatThrownBy(() -> service.resolve(MqttIdentityType.MAC, "aabbccddeeff"))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(identities);
    }

    private DeviceModel model(String code) {
        CommonCode type = CommonCode.createCommonCode(CodeGroup.createCodeGroup("MODEL_TYPE", "Model type"), code, code, 1);
        return DeviceModel.create(code, "Test", type, null);
    }

    private DeviceModelMqttPoint point(DeviceModel model, String path) {
        var type = CommonCode.createCommonCode(CodeGroup.createCodeGroup("DATA_POINT_TYPE", "Point type"),
                "TEMPERATURE", "Temperature", 1);
        return DeviceModelMqttPoint.create(model, path, "TEMPERATURE", type, null, 1.0, null, true);
    }
}
