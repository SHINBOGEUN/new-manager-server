package net.vivans.dcim.module.query.application;

import net.vivans.dcim.module.collectortask.application.CollectionGroupSpecService;
import net.vivans.dcim.module.collectortask.domain.repository.CollectionTaskRepository;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.repository.DeviceMonitoringPolicyRepository;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import net.vivans.dcim.module.devicemodel.application.DeviceModelPointCatalog;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModel;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModelProtocol;
import net.vivans.dcim.module.location.domain.model.LocationNode;
import net.vivans.dcim.module.query.config.CollectionStatusProperties;
import net.vivans.dcim.module.query.domain.PointQuery;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CollectionStatusProtocolScopeTest {

    @Test
    void mqttOnlyDevicesAreExcludedBeforeLimitButUnregisteredCollectorDevicesRemain() {
        DeviceRepository devices = mock(DeviceRepository.class);
        CollectionTaskRepository tasks = mock(CollectionTaskRepository.class);
        DeviceModelPointCatalog catalog = mock(DeviceModelPointCatalog.class);
        CollectionStatusQueryService service = new CollectionStatusQueryService(devices,
                mock(DeviceMonitoringPolicyRepository.class), tasks,
                mock(CollectionGroupSpecService.class), catalog, new CollectionStatusProperties(), mock(PointQuery.class));

        DeviceModel loraModel = model(1, "mqtt");
        CommonCode loraType = mock(CommonCode.class);
        when(loraType.getCode()).thenReturn("LORA_SENSOR");
        when(loraModel.getDeviceType()).thenReturn(loraType);
        Device mqttDevice = device(1, loraModel);
        Device snmpDevice = device(201, model(2, "snmp"));
        Device mixedDevice = device(202, model(3, "mqtt", "modbus"));

        when(tasks.findAll(null, null, null)).thenReturn(List.of());
        when(catalog.unitsByModelId(any())).thenReturn(Map.of());
        when(devices.findAll(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any()))
                .thenAnswer(invocation -> {
                    Pageable pageable = invocation.getArgument(6);
                    List<Device> content = pageable.getPageNumber() == 0
                            ? Collections.nCopies(200, mqttDevice) : List.of(snmpDevice, mixedDevice);
                    return new PageImpl<>(content, pageable, 202);
                });

        var status = service.getStatus(null);

        assertEquals(List.of(201, 202), status.devices().stream().map(row -> row.deviceId()).toList());
        assertEquals(2, status.summary().totalCount());
        assertEquals(2, status.summary().unregisteredCount());
    }

    private static DeviceModel model(int id, String... protocolCodes) {
        DeviceModel model = mock(DeviceModel.class);
        when(model.getId()).thenReturn(id);
        when(model.getName()).thenReturn("model-" + id);
        when(model.getManufacturer()).thenReturn("test");
        List<DeviceModelProtocol> protocols = java.util.Arrays.stream(protocolCodes).map(code -> {
            CommonCode type = mock(CommonCode.class);
            when(type.getCode()).thenReturn(code);
            DeviceModelProtocol protocol = mock(DeviceModelProtocol.class);
            when(protocol.getProtocolType()).thenReturn(type);
            return protocol;
        }).toList();
        when(model.getProtocols()).thenReturn(protocols);
        return model;
    }

    private static Device device(int id, DeviceModel model) {
        Device device = mock(Device.class);
        when(device.getId()).thenReturn(id);
        when(device.getName()).thenReturn("device-" + id);
        when(device.getDeviceModel()).thenReturn(model);
        when(device.isEnabled()).thenReturn(true);
        LocationNode location = mock(LocationNode.class);
        when(device.getLocationNode()).thenReturn(location);
        return device;
    }
}
