package net.vivans.dcim.module.lora.application;

import net.vivans.dcim.module.common.domain.model.CodeGroup;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModel;
import net.vivans.dcim.module.location.domain.model.LocationNode;
import net.vivans.dcim.module.lora.api.dto.LoraCollectionStatusResponse;
import net.vivans.dcim.module.lora.api.dto.LoraCollectionStatusRow;
import net.vivans.dcim.module.lora.api.dto.LoraMqttSourceResponse;
import net.vivans.dcim.module.lora.api.dto.LoraMqttSourceStatusResponse;
import net.vivans.dcim.module.mqtt.domain.model.DeviceMqttIdentity;
import net.vivans.dcim.module.mqtt.domain.model.DeviceModelMqttPoint;
import net.vivans.dcim.module.mqtt.domain.model.MqttIdentityType;
import net.vivans.dcim.module.lora.domain.model.LoraMqttSourceStatusType;
import net.vivans.dcim.module.lora.domain.model.LoraMqttSourceType;
import net.vivans.dcim.module.mqtt.domain.repository.DeviceMqttIdentityRepository;
import net.vivans.dcim.module.mqtt.domain.repository.DeviceModelMqttPointRepository;
import net.vivans.dcim.module.lora.infrastructure.influx.LoraCollectionInfluxQuery;
import net.vivans.dcim.module.lora.infrastructure.influx.LoraInfluxPointRecord;
import net.vivans.dcim.module.lora.infrastructure.sensordata.LoraRuntimeStatusClient;
import net.vivans.dcim.module.lora.infrastructure.sensordata.dto.LoraDeviceRuntimeStatusResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 상태 판정 우선순위(요청 기준): DISABLED > NO_MAPPING > NO_SOURCE > SOURCE_DOWN >
 * INFLUX_WRITE_FAILED > FIELD_ERROR > NEVER_SAVED > SAVED. INFLUX_WRITE_FAILED/FIELD_ERROR는
 * 저장 이력(lastSavedAt) 유무와 무관하게 NEVER_SAVED보다 우선한다.
 *
 * INFLUX_WRITE_FAILED/FIELD_ERROR 판정은 더 이상 DB 오류 이력이 아니라 Sensor Data의 런타임 상태
 * 캐시(GET /api/internal/lora/runtime-status)를 기준으로 하므로, 이 테스트에서는 그 클라이언트를
 * mock으로 대체한다({@link LoraRuntimeStatusClient}). 미등록(devEUI/deviceName)은 운영 정책상
 * 실패 이력으로 남기지 않으므로 이 서비스의 판정 대상이 아니다(별도 API/화면 없음).
 */
class LoraCollectionStatusServiceTest {

    private final DeviceMqttIdentityRepository mqttIdentityRepository = mock(DeviceMqttIdentityRepository.class);
    private final DeviceModelMqttPointRepository mqttPointRepository = mock(DeviceModelMqttPointRepository.class);
    private final LoraMqttSourceService loraMqttSourceService = mock(LoraMqttSourceService.class);
    private final LoraCollectionInfluxQuery influxQuery = mock(LoraCollectionInfluxQuery.class);
    private final LoraRuntimeStatusClient runtimeStatusClient = mock(LoraRuntimeStatusClient.class);

    private final LoraCollectionStatusService service = new LoraCollectionStatusService(
            mqttIdentityRepository, mqttPointRepository,
            loraMqttSourceService, influxQuery, runtimeStatusClient);

    @Test
    void noEnabledSource_deviceStatusIsNoSource() {
        DeviceModel model = model(100);
        DeviceMqttIdentity endpoint = endpoint(1, device(1, model, "센서-1"), MqttIdentityType.DEV_EUI, "AA01", true);

        when(mqttIdentityRepository.findAllOrderByIdAsc()).thenReturn(List.of(endpoint));
        when(loraMqttSourceService.getAll()).thenReturn(List.of()); // 등록된 소스 자체가 없음
        when(mqttPointRepository.findAllEnabledByDeviceModelIdIn(any())).thenReturn(mappingsFor(model));
        when(influxQuery.findRecentMqttPoints(any())).thenReturn(Map.of());

        LoraCollectionStatusResponse response = service.getCollectionStatus();

        assertThat(response.rows()).hasSize(1);
        assertThat(response.rows().get(0).status()).isEqualTo(LoraCollectionStatusService.STATUS_NO_SOURCE);
    }

    @Test
    void enabledSourceButNoneConnected_deviceStatusIsSourceDown() {
        DeviceModel model = model(100);
        DeviceMqttIdentity endpoint = endpoint(1, device(1, model, "센서-1"), MqttIdentityType.DEV_EUI, "AA01", true);

        when(mqttIdentityRepository.findAllOrderByIdAsc()).thenReturn(List.of(endpoint));
        when(loraMqttSourceService.getAll()).thenReturn(List.of(
                source(1, true, LoraMqttSourceStatusType.DISCONNECTED)));
        when(mqttPointRepository.findAllEnabledByDeviceModelIdIn(any())).thenReturn(mappingsFor(model));
        when(influxQuery.findRecentMqttPoints(any())).thenReturn(Map.of());

        LoraCollectionStatusResponse response = service.getCollectionStatus();

        assertThat(response.rows().get(0).status()).isEqualTo(LoraCollectionStatusService.STATUS_SOURCE_DOWN);
    }

    @Test
    void withHealthySource_perDeviceStatusFollowsPriorityOrder() {
        DeviceModel mappedModel = model(100);
        DeviceModel unmappedModel = model(200);

        Device disabledDevice = device(1, mappedModel, "장비-DISABLED");
        Device noMappingDevice = device(2, unmappedModel, "장비-NO_MAPPING");
        Device neverSavedDevice = device(3, mappedModel, "장비-NEVER_SAVED");
        Device influxFailedDevice = device(4, mappedModel, "장비-INFLUX_WRITE_FAILED");
        Device fieldErrorDevice = device(5, mappedModel, "장비-FIELD_ERROR");
        Device savedDevice = device(6, mappedModel, "장비-SAVED");

        List<DeviceMqttIdentity> endpoints = List.of(
                endpoint(1, disabledDevice, MqttIdentityType.DEV_EUI, "D1", false),
                endpoint(2, noMappingDevice, MqttIdentityType.DEV_EUI, "D2", true),
                endpoint(3, neverSavedDevice, MqttIdentityType.DEV_EUI, "D3", true),
                endpoint(4, influxFailedDevice, MqttIdentityType.DEV_EUI, "D4", true),
                endpoint(5, fieldErrorDevice, MqttIdentityType.DEV_EUI, "D5", true),
                endpoint(6, savedDevice, MqttIdentityType.DEV_EUI, "D6", true)
        );
        when(mqttIdentityRepository.findAllOrderByIdAsc()).thenReturn(endpoints);
        when(loraMqttSourceService.getAll()).thenReturn(List.of(
                source(1, true, LoraMqttSourceStatusType.CONNECTED)));
        when(mqttPointRepository.findAllEnabledByDeviceModelIdIn(any())).thenReturn(mappingsFor(mappedModel));

        Instant now = Instant.now();
        when(influxQuery.findRecentMqttPoints(any())).thenReturn(Map.of(
                4, List.of(new LoraInfluxPointRecord("TEMPERATURE", 21.0, now)),
                5, List.of(new LoraInfluxPointRecord("TEMPERATURE", 22.0, now)),
                6, List.of(new LoraInfluxPointRecord("TEMPERATURE", 23.0, now), new LoraInfluxPointRecord("HUMIDITY", 50.0, now))
        ));
        when(runtimeStatusClient.getAll()).thenReturn(List.of(
                new LoraDeviceRuntimeStatusResponse(4, now, now, 1, "INFLUX_WRITE_FAILED", "influx unavailable", now),
                new LoraDeviceRuntimeStatusResponse(5, now, now, 1, "FIELD_CONVERSION_FAILED", "변환 실패 필드 수=1", now)
        ));
        when(influxQuery.isAvailable()).thenReturn(true);

        LoraCollectionStatusResponse response = service.getCollectionStatus();

        assertThat(statusOf(response, 1)).isEqualTo(LoraCollectionStatusService.STATUS_DISABLED);
        assertThat(statusOf(response, 2)).isEqualTo(LoraCollectionStatusService.STATUS_NO_MAPPING);
        assertThat(statusOf(response, 3)).isEqualTo(LoraCollectionStatusService.STATUS_NEVER_SAVED);
        assertThat(statusOf(response, 4)).isEqualTo(LoraCollectionStatusService.STATUS_INFLUX_WRITE_FAILED);
        assertThat(statusOf(response, 5)).isEqualTo(LoraCollectionStatusService.STATUS_FIELD_ERROR);
        assertThat(statusOf(response, 6)).isEqualTo(LoraCollectionStatusService.STATUS_SAVED);

        LoraCollectionStatusRow savedRow = rowOf(response, 6);
        assertThat(savedRow.lastPointCount()).isEqualTo(2);
        assertThat(savedRow.lastMessageSavedAt()).isEqualTo(now);
        assertThat(savedRow.recentErrorReason()).isNull();

        assertThat(response.summary().totalDeviceCount()).isEqualTo(6);
        assertThat(response.summary().savedCount()).isEqualTo(1);
        assertThat(response.summary().problemCount()).isEqualTo(5);
    }

    @Test
    void deviceWithNoSaveHistoryButRuntimeError_showsErrorStatusNotNeverSaved() {
        // 회귀 시나리오: 첫 InfluxDB 저장이 아직 성공한 적 없는(lastSavedAt == null) 장비라도
        // Sensor Data runtime cache에 최근 INFLUX_WRITE_FAILED/FIELD_CONVERSION_FAILED가 있으면
        // NEVER_SAVED로 뭉뚱그리지 않고 실제 실패 원인을 그대로 노출해야 한다.
        DeviceModel mappedModel = model(100);
        Device influxFailedNoHistory = device(9, mappedModel, "장비-INFLUX_WRITE_FAILED-이력없음");
        Device fieldErrorNoHistory = device(10, mappedModel, "장비-FIELD_ERROR-이력없음");

        List<DeviceMqttIdentity> endpoints = List.of(
                endpoint(9, influxFailedNoHistory, MqttIdentityType.DEV_EUI, "D9", true),
                endpoint(10, fieldErrorNoHistory, MqttIdentityType.DEV_EUI, "D10", true)
        );
        when(mqttIdentityRepository.findAllOrderByIdAsc()).thenReturn(endpoints);
        when(loraMqttSourceService.getAll()).thenReturn(List.of(
                source(1, true, LoraMqttSourceStatusType.CONNECTED)));
        when(mqttPointRepository.findAllEnabledByDeviceModelIdIn(any())).thenReturn(mappingsFor(mappedModel));
        when(influxQuery.findRecentMqttPoints(any())).thenReturn(Map.of());

        Instant now = Instant.now();
        when(runtimeStatusClient.getAll()).thenReturn(List.of(
                new LoraDeviceRuntimeStatusResponse(9, now, null, null, "INFLUX_WRITE_FAILED", "influx unavailable", now),
                new LoraDeviceRuntimeStatusResponse(10, now, null, null, "FIELD_CONVERSION_FAILED", "변환 실패 필드 수=1", now)
        ));

        LoraCollectionStatusResponse response = service.getCollectionStatus();

        LoraCollectionStatusRow influxFailedRow = rowOf(response, 9);
        assertThat(influxFailedRow.status()).isEqualTo(LoraCollectionStatusService.STATUS_INFLUX_WRITE_FAILED);
        assertThat(influxFailedRow.lastMessageSavedAt()).isNull();
        assertThat(influxFailedRow.lastPointCount()).isNull();

        LoraCollectionStatusRow fieldErrorRow = rowOf(response, 10);
        assertThat(fieldErrorRow.status()).isEqualTo(LoraCollectionStatusService.STATUS_FIELD_ERROR);
        assertThat(fieldErrorRow.lastMessageSavedAt()).isNull();
        assertThat(fieldErrorRow.lastPointCount()).isNull();
    }

    @Test
    void runtimeErrorReasonAndTimestamp_areSurfacedOnRow() {
        DeviceModel mappedModel = model(100);
        Device deviceEntity = device(7, mappedModel, "장비-필드오류");
        DeviceMqttIdentity endpoint = endpoint(7, deviceEntity, MqttIdentityType.DEV_EUI, "D7", true);

        when(mqttIdentityRepository.findAllOrderByIdAsc()).thenReturn(List.of(endpoint));
        when(loraMqttSourceService.getAll()).thenReturn(List.of(
                source(1, true, LoraMqttSourceStatusType.CONNECTED)));
        when(mqttPointRepository.findAllEnabledByDeviceModelIdIn(any())).thenReturn(mappingsFor(mappedModel));

        Instant saved = Instant.now().minusSeconds(120);
        when(influxQuery.findRecentMqttPoints(any())).thenReturn(Map.of(
                7, List.of(new LoraInfluxPointRecord("TEMPERATURE", 20.0, saved))));

        Instant errorAt = Instant.now().minusSeconds(30);
        when(runtimeStatusClient.getAll()).thenReturn(List.of(
                new LoraDeviceRuntimeStatusResponse(7, errorAt, saved, 1, "FIELD_CONVERSION_FAILED", "변환 실패 필드 수=2", errorAt)));

        LoraCollectionStatusResponse response = service.getCollectionStatus();

        LoraCollectionStatusRow row = rowOf(response, 7);
        assertThat(row.status()).isEqualTo(LoraCollectionStatusService.STATUS_FIELD_ERROR);
        assertThat(row.recentErrorReason()).isEqualTo("FIELD_CONVERSION_FAILED - 변환 실패 필드 수=2");
        assertThat(row.recentErrorAt()).isEqualTo(errorAt);
    }

    @Test
    void staleUnrelatedRuntimeErrorCode_doesNotForceErrorStatus() {
        DeviceModel mappedModel = model(100);
        Device deviceEntity = device(8, mappedModel, "장비-무관오류");
        DeviceMqttIdentity endpoint = endpoint(8, deviceEntity, MqttIdentityType.DEV_EUI, "D8", true);

        when(mqttIdentityRepository.findAllOrderByIdAsc()).thenReturn(List.of(endpoint));
        when(loraMqttSourceService.getAll()).thenReturn(List.of(
                source(1, true, LoraMqttSourceStatusType.CONNECTED)));
        when(mqttPointRepository.findAllEnabledByDeviceModelIdIn(any())).thenReturn(mappingsFor(mappedModel));

        Instant saved = Instant.now().minusSeconds(60);
        when(influxQuery.findRecentMqttPoints(any())).thenReturn(Map.of(
                8, List.of(new LoraInfluxPointRecord("TEMPERATURE", 20.0, saved))));
        // NO_MAPPING은 Manager 판정 로직에서 INFLUX_WRITE_FAILED/FIELD_CONVERSION_FAILED로만 특별 취급하는
        // runtime 오류 코드가 아니다(매핑 존재 여부는 Manager가 findAllEnabledByDeviceModelIdIn으로 직접
        // 다시 확인한다). 매핑이 지금은 존재하고 저장 이력도 있으므로, 지나간 NO_MAPPING 런타임 오류가
        // 남아 있어도 상태가 강제로 오류로 바뀌지 않고 SAVED여야 한다.
        when(runtimeStatusClient.getAll()).thenReturn(List.of(
                new LoraDeviceRuntimeStatusResponse(8, saved, saved, 1, "NO_MAPPING", "모델 매핑 없음", saved)));

        LoraCollectionStatusResponse response = service.getCollectionStatus();

        LoraCollectionStatusRow row = rowOf(response, 8);
        assertThat(row.status()).isEqualTo(LoraCollectionStatusService.STATUS_SAVED);
    }

    private static String statusOf(LoraCollectionStatusResponse response, int deviceId) {
        return rowOf(response, deviceId).status();
    }

    private static LoraCollectionStatusRow rowOf(LoraCollectionStatusResponse response, int deviceId) {
        return response.rows().stream().filter(row -> row.deviceId() == deviceId).findFirst()
                .orElseThrow(() -> new AssertionError("row not found for deviceId=" + deviceId));
    }

    private DeviceModel model(int id) {
        CodeGroup group = CodeGroup.createCodeGroup("MODEL_TYPE", "모델 유형");
        CommonCode type = CommonCode.createCommonCode(group, "LORA_SENSOR", "LoRa 센서", 1);
        DeviceModel deviceModel = DeviceModel.create("model-" + id, "Dragino", type, null);
        ReflectionTestUtils.setField(deviceModel, "id", id);
        return deviceModel;
    }

    private Device device(int id, DeviceModel deviceModel, String name) {
        Device device = Device.create(deviceModel, unassignedLocation(), name, null);
        ReflectionTestUtils.setField(device, "id", id);
        return device;
    }

    private DeviceMqttIdentity endpoint(int id, Device device, MqttIdentityType idType, String externalId, boolean enabled) {
        DeviceMqttIdentity endpoint = DeviceMqttIdentity.create(device, idType, externalId, enabled);
        ReflectionTestUtils.setField(endpoint, "id", id);
        return endpoint;
    }

    private LocationNode unassignedLocation() {
        CodeGroup group = CodeGroup.createCodeGroup("LOCATION_TYPE", "위치 유형");
        CommonCode type = CommonCode.createCommonCode(group, "UNASSIGNED", "미배정", -1);
        return LocationNode.createRoot(Device.UNASSIGNED_LOCATION_CODE, type, "미배정");
    }

    /** mqttPointRepository.findAllEnabledByDeviceModelIdIn(...) 결과처럼 해당 모델에 활성 매핑이 1건 있음을 흉내낸다. */
    private List<DeviceModelMqttPoint> mappingsFor(DeviceModel deviceModel) {
        CodeGroup group = CodeGroup.createCodeGroup("DATA_POINT_TYPE", "데이터 포인트 유형");
        CommonCode dataPointType = CommonCode.createCommonCode(group, "TEMPERATURE", "온도", 1);
        DeviceModelMqttPoint point = DeviceModelMqttPoint.create(
                deviceModel, "object.TempC_SHT", "TEMPERATURE", dataPointType, null, 1.0, null, true);
        return List.of(point);
    }

    private LoraMqttSourceResponse source(int id, boolean enabled, LoraMqttSourceStatusType statusType) {
        LoraMqttSourceStatusResponse status = new LoraMqttSourceStatusResponse(
                statusType, null, null, null, 0, 0, 0, null);
        return new LoraMqttSourceResponse(id, "source-" + id, LoraMqttSourceType.CHIRPSTACK_LORA,
                "tcp://broker:1883", "application/#", null, null, enabled, 1, null, null, status);
    }
}
