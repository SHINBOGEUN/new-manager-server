package net.vivans.dcim.module.query.application;

import net.vivans.dcim.module.collectortask.application.CollectionGroupPlan;
import net.vivans.dcim.module.collectortask.application.CollectionGroupSpecService;
import net.vivans.dcim.module.collectortask.domain.model.CollectionTask;
import net.vivans.dcim.module.collectortask.domain.model.CollectionTaskDevice;
import net.vivans.dcim.module.collectortask.domain.model.CollectionTaskGroup;
import net.vivans.dcim.module.collectortask.domain.repository.CollectionTaskRepository;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.repository.DeviceMonitoringPolicyRepository;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import net.vivans.dcim.module.devicemodel.application.DeviceModelPointCatalog;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModel;
import net.vivans.dcim.module.location.domain.model.LocationNode;
import net.vivans.dcim.module.query.config.CollectionStatusProperties;
import net.vivans.dcim.module.query.domain.LastPoint;
import net.vivans.dcim.module.query.domain.PointQuery;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CollectionStatusModbusTest {

    @Test
    void sourceStatusUsesMappedTargetValueAndModbusProtocolOnly() {
        DeviceRepository devices = mock(DeviceRepository.class);
        CollectionTaskRepository tasks = mock(CollectionTaskRepository.class);
        CollectionGroupSpecService specs = mock(CollectionGroupSpecService.class);
        DeviceModelPointCatalog catalog = mock(DeviceModelPointCatalog.class);
        PointQuery points = mock(PointQuery.class);
        CollectionStatusQueryService service = new CollectionStatusQueryService(
                devices, mock(DeviceMonitoringPolicyRepository.class), tasks, specs, catalog,
                new CollectionStatusProperties(), points);

        DeviceModel model = mock(DeviceModel.class);
        when(model.getId()).thenReturn(10);
        Device source = mock(Device.class);
        when(source.getId()).thenReturn(7);
        when(source.getName()).thenReturn("meter");
        when(source.getDeviceModel()).thenReturn(model);
        when(source.isEnabled()).thenReturn(true);
        LocationNode location = mock(LocationNode.class);
        when(source.getLocationNode()).thenReturn(location);
        when(devices.findAll(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(source)));

        CommonCode modbus = mock(CommonCode.class);
        when(modbus.getCode()).thenReturn("modbus");
        CollectionTask task = mock(CollectionTask.class);
        when(task.getScriptType()).thenReturn(modbus);
        when(task.getId()).thenReturn(2);
        when(task.getDeviceModel()).thenReturn(model);
        when(task.isActive()).thenReturn(true);
        CollectionTaskGroup group = mock(CollectionTaskGroup.class);
        when(group.getId()).thenReturn(6);
        when(group.getCronExpression()).thenReturn("0 */1 * * * *");
        when(group.isActive()).thenReturn(true);
        when(task.getGroups()).thenReturn(List.of(group));
        CollectionTaskDevice mapping = mock(CollectionTaskDevice.class);
        when(mapping.getDevice()).thenReturn(source);
        when(group.getDevices()).thenReturn(List.of(mapping));
        when(tasks.findAll(null, null, null)).thenReturn(List.of(task));
        when(specs.generate(group)).thenReturn(new CollectionGroupPlan(new Object(), List.of(),
                Map.of(7, List.of(new CollectionGroupPlan.PointSource(101, "POWER", "W")))));
        when(catalog.unitsByModelId(any())).thenReturn(Map.of());
        when(points.findLast(eq(List.of(101)), eq(List.of("POWER")), any(), eq("modbus")))
                .thenReturn(List.of(new LastPoint(101, "POWER", 250, Instant.now())));

        var row = service.getStatus(null).devices().get(0);

        assertEquals("NORMAL", row.status());
        assertEquals(1, row.expectedPointCount());
        assertEquals(1, row.availablePointCount());
        assertEquals("POWER", row.latestValues().get(0).pointName());
        assertEquals("W", row.latestValues().get(0).unit());
        verify(points).findLast(eq(List.of(101)), eq(List.of("POWER")), any(), eq("modbus"));

        // 하나의 회선만 최신값이 있으면 전체 장비를 정상으로 표시하지 않는다.
        when(specs.generate(group)).thenReturn(new CollectionGroupPlan(new Object(), List.of(),
                Map.of(7, List.of(new CollectionGroupPlan.PointSource(101, "POWER", "W"),
                        new CollectionGroupPlan.PointSource(101, "CURRENT", "A")))));
        when(points.findLast(eq(List.of(101)), eq(List.of("POWER", "CURRENT")), any(), eq("modbus")))
                .thenReturn(List.of(new LastPoint(101, "POWER", 250, Instant.now())));

        var partial = service.getStatus(null);
        assertEquals("PARTIAL", partial.devices().get(0).status());
        assertEquals(2, partial.devices().get(0).expectedPointCount());
        assertEquals(1, partial.devices().get(0).availablePointCount());
        assertEquals(1, partial.summary().partialCount());
        assertTrue(partial.devices().get(0).technicalDetail().contains("CURRENT"));
    }
}
