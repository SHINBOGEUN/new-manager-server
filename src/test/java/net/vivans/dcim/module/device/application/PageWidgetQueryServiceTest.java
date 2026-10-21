package net.vivans.dcim.module.device.application;

import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.common.domain.repository.CommonCodeRepository;
import net.vivans.dcim.module.device.api.dto.PageWidgetCreateRequest;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import net.vivans.dcim.module.device.domain.repository.PageWidgetRepository;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModel;
import net.vivans.dcim.module.devicemodel.domain.repository.DeviceModelRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PageWidgetQueryServiceTest {

    @Mock
    private PageWidgetRepository pageWidgetRepository;
    @Mock
    private CommonCodeRepository commonCodeRepository;
    @Mock
    private DeviceRepository deviceRepository;
    @Mock
    private DeviceModelRepository deviceModelRepository;
    @Mock
    private DeviceMeasurementSourceCatalog sourceCatalog;
    @Mock
    private PageWidgetSpecializedSupport widgetSupport;
    @InjectMocks
    private PageWidgetQueryService service;

    @Test
    void createChart_rejectsCumulativeEnergyPoint() {
        CommonCode pageCode = org.mockito.Mockito.mock(CommonCode.class);
        Device device = org.mockito.Mockito.mock(Device.class);

        when(widgetSupport.findPageCode("POWER")).thenReturn(pageCode);
        when(pageCode.getId()).thenReturn(1);
        when(pageWidgetRepository.existsByPageCodeIdAndName(1, "누적 전력량")).thenReturn(false);
        when(deviceRepository.findById(7)).thenReturn(Optional.of(device));
        when(device.getId()).thenReturn(7);
        when(sourceCatalog.availableSources(org.mockito.ArgumentMatchers.anySet())).thenReturn(List.of(source("TOTAL_KWH", "kWh", "ENERGY")));

        PageWidgetCreateRequest request = new PageWidgetCreateRequest(
                "POWER", "누적 전력량", true, null, "chart",
                null, null, null, null, null,
                "devices", "per_device", "today", "5m",
                null, null, null,
                List.of(7), List.of(), List.of(), List.of("TOTAL_KWH"), null, null, null);

        assertThatThrownBy(() -> service.createWidget(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("누적 ENERGY 측정항목은 차트에서 사용할 수 없습니다")
                .hasMessageContaining("TOTAL_KWH");
    }

    @Test
    void createChart_rejectsMoreThanTwoUnits() {
        CommonCode pageCode = org.mockito.Mockito.mock(CommonCode.class);
        Device device = org.mockito.Mockito.mock(Device.class);

        when(widgetSupport.findPageCode("POWER")).thenReturn(pageCode);
        when(pageCode.getId()).thenReturn(1);
        when(pageWidgetRepository.existsByPageCodeIdAndName(1, "혼합 단위")).thenReturn(false);
        when(deviceRepository.findById(7)).thenReturn(Optional.of(device));
        when(device.getId()).thenReturn(7);
        when(sourceCatalog.availableSources(org.mockito.ArgumentMatchers.anySet())).thenReturn(List.of(
                source("TOTAL_WT", "W", "POWER"), source("IN_TEMP", "°C", "TEMPERATURE"),
                source("IN_HUM", "%", "HUMIDITY")));

        PageWidgetCreateRequest request = new PageWidgetCreateRequest(
                "POWER", "혼합 단위", true, null, "chart",
                null, null, null, null, null,
                "devices", "per_device", "last_3d", "15m",
                null, null, null,
                List.of(7), List.of(), List.of(), List.of("TOTAL_WT", "IN_TEMP", "IN_HUM"), null, null, null);

        assertThatThrownBy(() -> service.createWidget(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("최대 두 단위");
    }

    private static DeviceMeasurementSourceCatalog.Source source(String name, String unit, String type) {
        return new DeviceMeasurementSourceCatalog.Source(7, "PDU", 3, "snmp", name, unit,
                type, 7, "MODEL_POINT", false);
    }
}
