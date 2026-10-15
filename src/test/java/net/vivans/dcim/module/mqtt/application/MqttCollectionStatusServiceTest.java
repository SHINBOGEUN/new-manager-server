package net.vivans.dcim.module.mqtt.application;

import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModel;
import net.vivans.dcim.module.mqtt.domain.model.*;
import net.vivans.dcim.module.mqtt.domain.repository.*;
import net.vivans.dcim.module.mqtt.api.dto.MqttSourceResponse;
import net.vivans.dcim.module.mqtt.api.dto.MqttSourceStatusResponse;
import net.vivans.dcim.module.mqtt.infrastructure.influx.MqttCollectionInfluxQuery;
import net.vivans.dcim.module.lora.infrastructure.influx.LoraInfluxPointRecord;
import net.vivans.dcim.module.lora.infrastructure.sensordata.LoraRuntimeStatusClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.*;

class MqttCollectionStatusServiceTest {
    final DeviceMqttIdentityRepository identities = mock(DeviceMqttIdentityRepository.class);
    final DeviceModelMqttPointRepository points = mock(DeviceModelMqttPointRepository.class);
    final MqttSourceService sources = mock(MqttSourceService.class);
    final MqttCollectionInfluxQuery influx = mock(MqttCollectionInfluxQuery.class);
    final LoraRuntimeStatusClient runtime = mock(LoraRuntimeStatusClient.class);
    final MqttCollectionStatusService service = new MqttCollectionStatusService(identities, points, sources, influx, runtime);
    final Device device = mock(Device.class);
    final DeviceModel model = mock(DeviceModel.class);
    final DeviceMqttIdentity identity = mock(DeviceMqttIdentity.class);
    @BeforeEach void setup() {
        when(device.getId()).thenReturn(33);
        when(device.getDeviceModel()).thenReturn(model);
        when(model.getId()).thenReturn(12);
        when(identity.getDevice()).thenReturn(device);
        when(identity.getIdentityType()).thenReturn(MqttIdentityType.MAC);
        when(identity.isEnabled()).thenReturn(true);
        when(identities.findAllOrderByIdAsc()).thenReturn(List.of(identity));
        var point = mock(DeviceModelMqttPoint.class);
        when(point.getDeviceModel()).thenReturn(model);
        when(points.findAllEnabledByDeviceModelIdIn(any())).thenReturn(List.of(point));
        when(influx.isAvailable()).thenReturn(true);
        when(influx.findRecentMqttPoints(any())).thenReturn(Map.of());
        when(runtime.getAll()).thenReturn(List.of());
    }
    MqttSourceResponse source(MqttSourceType type) {
        var source = mock(MqttSourceResponse.class);
        var status = mock(MqttSourceStatusResponse.class);
        when(source.sourceType()).thenReturn(type);
        when(source.enabled()).thenReturn(true);
        when(source.status()).thenReturn(status);
        when(status.status()).thenReturn(MqttSourceStatusType.CONNECTED);
        return source;
    }
    @Test void includesGpuAndCountsAllSixComponentValues() {
        var agentSource = source(MqttSourceType.AGENT);
        when(sources.getAll()).thenReturn(List.of(agentSource));
        Instant now = Instant.now();
        when(influx.findRecentMqttPoints(any())).thenReturn(Map.of(33, List.of(
                new LoraInfluxPointRecord("power", 1D, now), new LoraInfluxPointRecord("power", 2D, now),
                new LoraInfluxPointRecord("power", 3D, now), new LoraInfluxPointRecord("gpu_temperature", 59D, now),
                new LoraInfluxPointRecord("gpu_temperature", 60D, now), new LoraInfluxPointRecord("gpu_temperature", 61D, now))));
        var response = service.getCollectionStatus();
        assertThat(response.rows().get(0).deviceId()).isEqualTo(33);
        assertThat(response.rows().get(0).lastPointCount()).isEqualTo(6);
        assertThat(response.rows().get(0).status()).isEqualTo("SAVED");
    }
    @Test void loraConnectionDoesNotProveAgentSourceAvailable() {
        var loraSource = source(MqttSourceType.CHIRPSTACK_LORA);
        when(sources.getAll()).thenReturn(List.of(loraSource));
        assertThat(service.getCollectionStatus().rows().get(0).status()).isEqualTo("NO_SOURCE");
    }
    @Test void queryFailureIsNotNeverSaved() {
        var agentSource = source(MqttSourceType.AGENT);
        when(sources.getAll()).thenReturn(List.of(agentSource));
        when(influx.findRecentMqttPoints(any())).thenThrow(new IllegalStateException("offline"));
        var response = service.getCollectionStatus();
        assertThat(response.summary().influxAvailable()).isFalse();
        assertThat(response.rows().get(0).status()).isEqualTo("INFLUX_UNAVAILABLE");
    }
    @Test void multipleIdentitiesCountOneDevice() {
        when(identities.findAllOrderByIdAsc()).thenReturn(List.of(identity, identity));
        when(sources.getAll()).thenReturn(List.of());
        assertThat(service.getCollectionStatus().summary().totalDeviceCount()).isEqualTo(1);
    }
}
