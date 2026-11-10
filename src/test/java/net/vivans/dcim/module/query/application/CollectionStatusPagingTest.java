package net.vivans.dcim.module.query.application;

import net.vivans.dcim.module.collectortask.application.CollectionGroupSpecService;
import net.vivans.dcim.module.collectortask.domain.repository.CollectionTaskRepository;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.repository.DeviceMonitoringPolicyRepository;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import net.vivans.dcim.module.devicemodel.application.DeviceModelPointCatalog;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModel;
import net.vivans.dcim.module.location.domain.model.LocationNode;
import net.vivans.dcim.module.query.config.CollectionStatusProperties;
import net.vivans.dcim.module.query.domain.PointQuery;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CollectionStatusPagingTest {
    @Test void includesDevicesBeyondTheFirstTwoHundredInRowsAndSummary() {
        var devices = mock(DeviceRepository.class);
        var policies = mock(DeviceMonitoringPolicyRepository.class);
        var tasks = mock(CollectionTaskRepository.class);
        var catalog = mock(DeviceModelPointCatalog.class);
        var model = mock(DeviceModel.class);
        var location = mock(LocationNode.class);
        when(model.getId()).thenReturn(1);
        when(model.getName()).thenReturn("model");
        when(model.getProtocols()).thenReturn(List.of());
        var all = IntStream.rangeClosed(1, 201).mapToObj(id -> {
            Device d = mock(Device.class);
            when(d.getId()).thenReturn(id);
            when(d.getName()).thenReturn("device-" + id);
            when(d.getDeviceModel()).thenReturn(model);
            when(d.getLocationNode()).thenReturn(location);
            return d;
        }).toList();
        when(devices.findAll(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any()))
                .thenAnswer(invocation -> {
                    org.springframework.data.domain.Pageable page = invocation.getArgument(6);
                    int start = (int) page.getOffset();
                    return new PageImpl<>(all.subList(start, Math.min(start + page.getPageSize(), all.size())), page, all.size());
                });
        when(policies.findAll()).thenReturn(List.of());
        when(tasks.findAll(null, null, null)).thenReturn(List.of());
        when(catalog.unitsByModelId(any())).thenReturn(Map.of());
        var service = new CollectionStatusQueryService(devices, policies, tasks, mock(CollectionGroupSpecService.class),
                catalog, new CollectionStatusProperties(), mock(PointQuery.class));
        var result = service.getStatus(null);
        assertThat(result.devices()).hasSize(201);
        assertThat(result.summary().totalCount()).isEqualTo(201);
        assertThat(result.devices()).anyMatch(row -> row.deviceId() == 201);
    }
}
