package net.vivans.dcim.module.device.application;

import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.common.domain.repository.CommonCodeRepository;
import net.vivans.dcim.module.device.api.dto.PageWidgetCreateRequest;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import net.vivans.dcim.module.device.domain.repository.PageWidgetRepository;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModel;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModelSnmpPoint;
import net.vivans.dcim.module.devicemodel.domain.repository.DeviceModelRepository;
import net.vivans.dcim.module.devicemodel.domain.repository.DeviceModelSnmpPointRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
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
    private DeviceModelSnmpPointRepository deviceModelSnmpPointRepository;
    @InjectMocks
    private PageWidgetQueryService service;

    @Test
    void createChart_rejectsCumulativeEnergyPoint() {
        CommonCode pageCode = org.mockito.Mockito.mock(CommonCode.class);
        CommonCode energyType = org.mockito.Mockito.mock(CommonCode.class);
        Device device = org.mockito.Mockito.mock(Device.class);
        DeviceModel model = org.mockito.Mockito.mock(DeviceModel.class);
        DeviceModelSnmpPoint energyPoint = org.mockito.Mockito.mock(DeviceModelSnmpPoint.class);

        when(commonCodeRepository.findByCodeGroupGroupKeyAndCode("DEVICE_PAGE", "POWER"))
                .thenReturn(Optional.of(pageCode));
        when(pageCode.getId()).thenReturn(1);
        when(pageWidgetRepository.existsByPageCodeIdAndName(1, "누적 전력량")).thenReturn(false);
        when(deviceRepository.findById(7)).thenReturn(Optional.of(device));
        when(device.getDeviceModel()).thenReturn(model);
        when(model.getId()).thenReturn(3);
        when(deviceModelSnmpPointRepository.findAllEnabledByDeviceModelIds(anyCollection()))
                .thenReturn(List.of(energyPoint));
        when(energyPoint.getName()).thenReturn("TOTAL_KWH");
        when(energyPoint.getDataPointType()).thenReturn(energyType);
        when(energyType.getCode()).thenReturn("ENERGY");

        PageWidgetCreateRequest request = new PageWidgetCreateRequest(
                "POWER", "누적 전력량", true, null, "chart",
                null, null, null, null, null,
                "devices", "per_device", "today", "5m",
                null, null, null,
                List.of(7), List.of(), List.of(), List.of("TOTAL_KWH"), null, null);

        assertThatThrownBy(() -> service.createWidget(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("누적 ENERGY 측정항목은 차트에서 사용할 수 없습니다")
                .hasMessageContaining("TOTAL_KWH");
    }

    @Test
    void createChart_rejectsMoreThanTwoUnits() {
        CommonCode pageCode = org.mockito.Mockito.mock(CommonCode.class);
        Device device = org.mockito.Mockito.mock(Device.class);
        DeviceModel model = org.mockito.Mockito.mock(DeviceModel.class);
        DeviceModelSnmpPoint power = org.mockito.Mockito.mock(DeviceModelSnmpPoint.class);
        DeviceModelSnmpPoint temperature = org.mockito.Mockito.mock(DeviceModelSnmpPoint.class);
        DeviceModelSnmpPoint humidity = org.mockito.Mockito.mock(DeviceModelSnmpPoint.class);

        when(commonCodeRepository.findByCodeGroupGroupKeyAndCode("DEVICE_PAGE", "POWER"))
                .thenReturn(Optional.of(pageCode));
        when(pageCode.getId()).thenReturn(1);
        when(pageWidgetRepository.existsByPageCodeIdAndName(1, "혼합 단위")).thenReturn(false);
        when(deviceRepository.findById(7)).thenReturn(Optional.of(device));
        when(device.getDeviceModel()).thenReturn(model);
        when(model.getId()).thenReturn(3);
        when(power.getName()).thenReturn("TOTAL_WT");
        when(power.getUnit()).thenReturn("W");
        when(temperature.getName()).thenReturn("IN_TEMP");
        when(temperature.getUnit()).thenReturn("°C");
        when(humidity.getName()).thenReturn("IN_HUM");
        when(humidity.getUnit()).thenReturn("%");
        when(deviceModelSnmpPointRepository.findAllEnabledByDeviceModelIds(anyCollection()))
                .thenReturn(List.of(power, temperature, humidity));

        PageWidgetCreateRequest request = new PageWidgetCreateRequest(
                "POWER", "혼합 단위", true, null, "chart",
                null, null, null, null, null,
                "devices", "per_device", "last_3d", "15m",
                null, null, null,
                List.of(7), List.of(), List.of(), List.of("TOTAL_WT", "IN_TEMP", "IN_HUM"), null, null);

        assertThatThrownBy(() -> service.createWidget(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("최대 두 단위");
    }
}
