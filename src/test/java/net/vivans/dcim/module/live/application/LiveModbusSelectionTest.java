package net.vivans.dcim.module.live.application;

import net.vivans.dcim.module.collectortask.application.CollectionGroupModbusSpec;
import net.vivans.dcim.module.collectortask.application.CollectionGroupSpecService;
import net.vivans.dcim.module.device.api.dto.DeviceCapabilityResponse;
import net.vivans.dcim.module.device.application.DeviceCapabilityQueryService;
import net.vivans.dcim.module.device.application.DeviceMeasurementSourceCatalog;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModel;
import net.vivans.dcim.module.live.api.dto.LiveDeviceResponse;
import net.vivans.dcim.module.live.api.dto.LivePointResponse;
import net.vivans.dcim.module.live.api.dto.LiveSelectionItemRequest;
import net.vivans.dcim.module.location.domain.model.LocationNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LiveModbusSelectionTest {
    private final DeviceCapabilityQueryService capabilities = mock(DeviceCapabilityQueryService.class);
    private final DeviceMeasurementSourceCatalog catalog = mock(DeviceMeasurementSourceCatalog.class);
    private final DeviceRepository devices = mock(DeviceRepository.class);
    private final CollectionGroupSpecService groupSpecs = mock(CollectionGroupSpecService.class);

    private static CollectionGroupModbusSpec.ModbusTarget modbusTarget() {
        var bit = new CollectionGroupModbusSpec.ModbusBitField("VALVE_1", 0, 2,
                Map.of("1", 0L, "2", 1L), -1L);
        var point = new CollectionGroupModbusSpec.ModbusPoint("VALVE_RAW", "HOLDING", 0,
                "UINT16", null, null, null, List.of(bit));
        return new CollectionGroupModbusSpec.ModbusTarget(31, "127.0.0.1", 502, 1, List.of(point));
    }

    @Test
    void selectablePointsUseStorageDeviceAndSourceMapping() {
        Device source = mock(Device.class);
        Device target = mock(Device.class);
        DeviceModel model = mock(DeviceModel.class);
        LocationNode location = mock(LocationNode.class);
        when(capabilities.getCapabilities(null, null, null)).thenReturn(List.<DeviceCapabilityResponse>of());
        when(catalog.availableSources()).thenReturn(List.of(
                new DeviceMeasurementSourceCatalog.Source(31, "CHILLER", 10, "modbus",
                        "VALVE_1", null, "STATUS", 17, "BIT_FIELD", false)));
        when(devices.findById(17)).thenReturn(Optional.of(source));
        when(devices.findById(31)).thenReturn(Optional.of(target));
        when(groupSpecs.previewModbus(source)).thenReturn(
                new CollectionGroupSpecService.ModbusPreviewPlan(List.of(modbusTarget()), List.of()));
        when(target.getName()).thenReturn("CHILLER");
        when(target.getLocationNode()).thenReturn(location);
        when(target.getDeviceModel()).thenReturn(model);
        when(location.getName()).thenReturn("Room");
        when(model.getId()).thenReturn(10);
        when(model.getName()).thenReturn("CHILLER");
        when(source.getName()).thenReturn("ACCURA");

        var query = new LiveTelemetryQueryService(capabilities, catalog, devices, groupSpecs);
        assertThat(query.getSelectableDevices()).singleElement().satisfies(device -> {
            assertThat(device.deviceId()).isEqualTo(31);
            assertThat(device.points()).singleElement().satisfies(point -> {
                assertThat(point.protocol()).isEqualTo("modbus");
                assertThat(point.sourceDeviceId()).isEqualTo(17);
                assertThat(point.name()).isEqualTo("VALVE_1");
            });
        });
        assertThat(query.normalizeAndValidate(List.of(
                new LiveSelectionItemRequest(31, List.of("VALVE_1"), "modbus", 17))))
                .singleElement().satisfies(item -> assertThat(item.sourceDeviceId()).isEqualTo(17));
    }

    @Test
    void derivedPointReadsParentButPublishesOnlySelectedBitField() {
        Device source = mock(Device.class);
        when(devices.findById(17)).thenReturn(Optional.of(source));
        when(groupSpecs.previewModbus(source)).thenReturn(
                new CollectionGroupSpecService.ModbusPreviewPlan(List.of(modbusTarget()), List.of()));
        LiveDeviceResponse selectable = new LiveDeviceResponse(31, "CHILLER", "Room", 10,
                "CHILLER", List.of(new LivePointResponse("VALVE_1", null, "modbus", 17, "ACCURA")));
        LiveTelemetryQueryService query = mock(LiveTelemetryQueryService.class);
        when(query.getSelectableDevices()).thenReturn(List.of(selectable));
        when(capabilities.getCapabilities(null, null, null)).thenReturn(List.<DeviceCapabilityResponse>of());

        var service = new LiveTelemetrySpecService(capabilities, devices, groupSpecs, query);
        var spec = service.build(List.of(new LiveSelectionItemRequest(31, List.of("VALVE_1"), "modbus", 17)));

        assertThat(spec.protocol()).isEqualTo("modbus");
        assertThat(spec.targets()).isEmpty();
        assertThat(spec.modbusTargets()).singleElement().satisfies(target -> {
            assertThat(target.sourceDeviceId()).isEqualTo(17);
            assertThat(target.target().points()).singleElement().satisfies(point -> {
                assertThat(point.name()).isEqualTo("VALVE_RAW");
                assertThat(point.bitFields()).singleElement().satisfies(bit ->
                        assertThat(bit.name()).isEqualTo("VALVE_1"));
            });
            assertThat(target.units()).containsKey("VALVE_1").doesNotContainKey("VALVE_RAW");
        });
    }
    @Test
    void usesConfiguredSnmpAndLiveCollectionSettings() {
        var query = mock(LiveTelemetryQueryService.class);
        when(query.getSelectableDevices()).thenReturn(List.of());
        when(capabilities.getCapabilities(null, null, null)).thenReturn(List.of());
        var service = new LiveTelemetrySpecService(capabilities, devices, groupSpecs, query);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "community", "private-community");
        org.springframework.test.util.ReflectionTestUtils.setField(service, "intervalMs", 2500);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "timeoutMs", 1600);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "retries", 2);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "maxConcurrency", 4);
        var spec = service.build(List.of());
        assertThat(spec.community()).isEqualTo("private-community");
        assertThat(spec.intervalMs()).isEqualTo(2500);
        assertThat(spec.timeoutMs()).isEqualTo(1600);
        assertThat(spec.retries()).isEqualTo(2);
        assertThat(spec.maxConcurrency()).isEqualTo(4);
    }
}
