package net.vivans.dcim.module.query.application;

import net.vivans.dcim.module.common.domain.model.CodeGroup;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.model.DevicePageCodes;
import net.vivans.dcim.module.device.domain.model.PageWidget;
import net.vivans.dcim.module.device.domain.model.PageWidgetCountMode;
import net.vivans.dcim.module.device.domain.model.PageWidgetQueryKind;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import net.vivans.dcim.module.device.domain.repository.PageWidgetRepository;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModel;
import net.vivans.dcim.module.query.api.dto.CountWidgetResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CountQueryServiceTest {

    @Mock
    private PageWidgetRepository pageWidgetRepository;

    @Mock
    private DeviceRepository deviceRepository;

    @InjectMocks
    private CountQueryService service;

    @Test
    void byModelMode_returnsTotalAndBreakdown() {
        DeviceModel modelA = model(10, "AP8959", "APC");
        DeviceModel modelB = model(20, "30XA", "Carrier");
        PageWidget widget = countWidget(12, PageWidgetCountMode.by_model, null);
        when(pageWidgetRepository.findById(12)).thenReturn(Optional.of(widget));
        stubEnabledDevices(
                device(1, modelA), device(2, modelA), device(3, modelB));

        CountWidgetResponse response = service.getCount(12, null, null);

        assertThat(response.countMode()).isEqualTo("by_model");
        assertThat(response.count()).isEqualTo(3);
        assertThat(response.byModel()).hasSize(2);
        assertThat(response.byModel().get(0).count()).isEqualTo(2);
    }

    @Test
    void totalMode_returnsCountOnly() {
        DeviceModel modelA = model(10, "AP8959", "APC");
        PageWidget widget = countWidget(12, PageWidgetCountMode.total, null);
        when(pageWidgetRepository.findById(12)).thenReturn(Optional.of(widget));
        stubEnabledDevices(device(1, modelA), device(2, modelA));

        CountWidgetResponse response = service.getCount(12, null, null);

        assertThat(response.countMode()).isEqualTo("total");
        assertThat(response.count()).isEqualTo(2);
        assertThat(response.byModel()).isEmpty();
    }

    @Test
    void modelMode_filtersSingleModel() {
        DeviceModel modelA = model(10, "AP8959", "APC");
        DeviceModel modelB = model(20, "30XA", "Carrier");
        PageWidget widget = countWidget(12, PageWidgetCountMode.model, 10);
        when(pageWidgetRepository.findById(12)).thenReturn(Optional.of(widget));
        stubEnabledDevices(
                device(1, modelA), device(2, modelA), device(3, modelB));

        CountWidgetResponse response = service.getCount(12, null, null);

        assertThat(response.countMode()).isEqualTo("model");
        assertThat(response.countModelId()).isEqualTo(10);
        assertThat(response.count()).isEqualTo(2);
        assertThat(response.byModel()).hasSize(1);
        assertThat(response.byModel().get(0).modelId()).isEqualTo(10);
    }

    @Test
    void countsOnlyEnabledDevicesFromRepository() {
        DeviceModel modelA = model(10, "AP8959", "APC");
        PageWidget widget = countWidget(12, PageWidgetCountMode.total, null);
        when(pageWidgetRepository.findById(12)).thenReturn(Optional.of(widget));
        stubEnabledDevices(device(1, modelA));

        CountWidgetResponse response = service.getCount(12, null, null);

        assertThat(response.count()).isEqualTo(1);
    }

    @Test
    void overrideMode_usesQueryParam() {
        DeviceModel modelA = model(10, "AP8959", "APC");
        PageWidget widget = countWidget(12, PageWidgetCountMode.by_model, null);
        when(pageWidgetRepository.findById(12)).thenReturn(Optional.of(widget));
        stubEnabledDevices(device(1, modelA));

        CountWidgetResponse response = service.getCount(12, "total", null);

        assertThat(response.countMode()).isEqualTo("total");
        assertThat(response.byModel()).isEmpty();
    }

    @Test
    void rejectsNonCountWidget() {
        PageWidget widget = mock(PageWidget.class);
        when(widget.getQueryKind()).thenReturn(PageWidgetQueryKind.last);
        when(pageWidgetRepository.findById(5)).thenReturn(Optional.of(widget));

        assertThatThrownBy(() -> service.getCount(5, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("queryKind must be count");
    }

    private void stubEnabledDevices(Device... devices) {
        when(deviceRepository.findAllEnabled()).thenReturn(List.of(devices));
    }

    private static PageWidget countWidget(
            int id,
            PageWidgetCountMode countMode,
            Integer countModelId
    ) {
        PageWidget widget = mock(PageWidget.class);
        when(widget.getId()).thenReturn(id);
        when(widget.getName()).thenReturn("장비수");
        when(widget.getQueryKind()).thenReturn(PageWidgetQueryKind.count);
        when(widget.isEnabled()).thenReturn(true);
        when(widget.getCountMode()).thenReturn(countMode);
        when(widget.getCountModelId()).thenReturn(countModelId);
        CommonCode page = pageCode("dashboard");
        when(widget.getPageCode()).thenReturn(page);
        return widget;
    }

    private static Device device(int id, DeviceModel model) {
        Device device = mock(Device.class);
        lenient().when(device.getId()).thenReturn(id);
        lenient().when(device.getDeviceModel()).thenReturn(model);
        return device;
    }

    private static DeviceModel model(int id, String name, String manufacturer) {
        DeviceModel model = mock(DeviceModel.class);
        lenient().when(model.getId()).thenReturn(id);
        lenient().when(model.getName()).thenReturn(name);
        lenient().when(model.getManufacturer()).thenReturn(manufacturer);
        return model;
    }

    private static CommonCode pageCode(String code) {
        CodeGroup group = mock(CodeGroup.class);
        lenient().when(group.getGroupKey()).thenReturn(DevicePageCodes.DEVICE_PAGE_GROUP_KEY);
        CommonCode page = mock(CommonCode.class);
        when(page.getCode()).thenReturn(code);
        lenient().when(page.getCodeGroup()).thenReturn(group);
        return page;
    }
}
