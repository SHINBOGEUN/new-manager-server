package net.vivans.dcim.module.query.application;

import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.model.PageWidget;
import net.vivans.dcim.module.device.domain.model.PageWidgetPueSource;
import net.vivans.dcim.module.device.domain.model.PageWidgetPueSourceRole;
import net.vivans.dcim.module.device.domain.model.PageWidgetQueryKind;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import net.vivans.dcim.module.device.domain.repository.PageWidgetRepository;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModel;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModelProtocol;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModelSnmpPoint;
import net.vivans.dcim.module.devicemodel.domain.repository.DeviceModelSnmpPointRepository;
import net.vivans.dcim.module.query.api.dto.PueQueryRequest;
import net.vivans.dcim.module.query.api.dto.PueQueryResponse;
import net.vivans.dcim.module.query.api.dto.PueSourceRequest;
import net.vivans.dcim.module.query.domain.LastPoint;
import net.vivans.dcim.module.query.domain.PointQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PueQueryServiceTest {

    private static final Instant VALUE_TIME = Instant.parse("2026-09-07T00:00:00Z");

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private DeviceModelSnmpPointRepository deviceModelSnmpPointRepository;

    @Mock
    private PointQuery pointQuery;

    @Mock
    private PageWidgetRepository pageWidgetRepository;

    @InjectMocks
    private PueQueryService service;

    @Test
    void calculatesPueFromEachDevicesSelectedPowerPoint() {
        prepareDevicesAndPowerPoints();
        when(pointQuery.findLastInRange(anyList(), anyList(), any(), any())).thenReturn(List.of(
                new LastPoint(1, "TOTAL_WT", 100.0, VALUE_TIME),
                new LastPoint(2, "T_WT", 50.0, VALUE_TIME),
                new LastPoint(10, "POWER", 50.0, VALUE_TIME)
        ));

        PueQueryResponse response = service.getPue(request());

        assertThat(response.value()).isEqualByComparingTo("3.0000");
        assertThat(response.totalPower()).isEqualByComparingTo("150.00");
        assertThat(response.coolerPower()).isEqualByComparingTo("50.00");
        assertThat(response.unit()).isEqualTo("W");
        assertThat(response.complete()).isTrue();
        assertThat(response.calculationStatus()).isEqualTo("OK");
        assertThat(response.devices()).extracting(d -> d.pointName())
                .containsExactly("TOTAL_WT", "T_WT", "POWER");
    }

    @Test
    void returnsNullPueWhenAnyRequestedDeviceHasNoData() {
        prepareDevicesAndPowerPoints();
        when(pointQuery.findLastInRange(anyList(), anyList(), any(), any())).thenReturn(List.of(
                new LastPoint(1, "TOTAL_WT", 100.0, VALUE_TIME),
                new LastPoint(10, "POWER", 50.0, VALUE_TIME)
        ));

        PueQueryResponse response = service.getPue(request());

        assertThat(response.value()).isNull();
        assertThat(response.totalPower()).isNull();
        assertThat(response.coolerPower()).isEqualByComparingTo("50.00");
        assertThat(response.complete()).isFalse();
        assertThat(response.calculationStatus()).isEqualTo("MISSING_DATA");
        assertThat(response.missingDeviceIds()).containsExactly(2);
    }

    @Test
    void returnsNullPueWhenCoolerPowerIsZero() {
        prepareDevicesAndPowerPoints();
        when(pointQuery.findLastInRange(anyList(), anyList(), any(), any())).thenReturn(List.of(
                new LastPoint(1, "TOTAL_WT", 100.0, VALUE_TIME),
                new LastPoint(2, "T_WT", 50.0, VALUE_TIME),
                new LastPoint(10, "POWER", 0.0, VALUE_TIME)
        ));

        PueQueryResponse response = service.getPue(request());

        assertThat(response.value()).isNull();
        assertThat(response.complete()).isTrue();
        assertThat(response.calculationStatus()).isEqualTo("ZERO_COOLER_POWER");
    }

    @Test
    void rejectsDeviceAssignedToBothRoles() {
        PueQueryRequest request = new PueQueryRequest(
                List.of(new PueSourceRequest(1, "TOTAL_WT")),
                List.of(new PueSourceRequest(1, "POWER")),
                "last_24h");

        assertThatThrownBy(() -> service.getPue(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("both totalSources and coolerSources");
    }

    @Test
    void rejectsPointThatIsNotPower() {
        Device device = device(1, 101);
        Device cooler = device(10, 110);
        when(deviceRepository.findById(1)).thenReturn(Optional.of(device));
        when(deviceRepository.findById(10)).thenReturn(Optional.of(cooler));
        DeviceModelSnmpPoint temperaturePoint = point(101, "TEMP", "C", "TEMPERATURE");
        DeviceModelSnmpPoint coolerPoint = point(110, "POWER", "W", "POWER");
        when(deviceModelSnmpPointRepository.findAllEnabledByDeviceModelIds(any()))
                .thenReturn(List.of(temperaturePoint, coolerPoint));
        PueQueryRequest request = new PueQueryRequest(
                List.of(new PueSourceRequest(1, "TEMP")),
                List.of(new PueSourceRequest(10, "POWER")),
                "last_24h");

        assertThatThrownBy(() -> service.getPue(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DATA_POINT_TYPE=POWER");
    }

    @Test
    void rejectsPowerPointsWithDifferentUnits() {
        Device total = device(1, 101);
        Device cooler = device(10, 110);
        when(deviceRepository.findById(1)).thenReturn(Optional.of(total));
        when(deviceRepository.findById(10)).thenReturn(Optional.of(cooler));
        DeviceModelSnmpPoint totalPoint = point(101, "TOTAL_WT", "W", "POWER");
        DeviceModelSnmpPoint coolerPoint = point(110, "POWER", "kW", "POWER");
        when(deviceModelSnmpPointRepository.findAllEnabledByDeviceModelIds(any()))
                .thenReturn(List.of(totalPoint, coolerPoint));
        PueQueryRequest request = new PueQueryRequest(
                List.of(new PueSourceRequest(1, "TOTAL_WT")),
                List.of(new PueSourceRequest(10, "POWER")),
                "last_24h");

        assertThatThrownBy(() -> service.getPue(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("same unit");
    }

    @Test
    void savedWidgetTreatsOldValueAsMissingUsingFreshnessMinutes() {
        Device total = device(1, 101);
        Device cooler = device(10, 110);
        PageWidgetPueSource totalSource = pueSource(total, "TOTAL_WT", PageWidgetPueSourceRole.total);
        PageWidgetPueSource coolerSource = pueSource(cooler, "POWER", PageWidgetPueSourceRole.cooler);
        PageWidget widget = mock(PageWidget.class);
        when(pageWidgetRepository.findById(30)).thenReturn(Optional.of(widget));
        when(widget.getQueryKind()).thenReturn(PageWidgetQueryKind.pue);
        when(widget.isEnabled()).thenReturn(true);
        when(widget.getPueRangePreset()).thenReturn(net.vivans.dcim.module.device.domain.model.PageWidgetChartRangePreset.last_24h);
        when(widget.getPueFreshnessMinutes()).thenReturn(15);
        when(widget.getPueSources()).thenReturn(java.util.Set.of(totalSource, coolerSource));
        when(deviceRepository.findById(1)).thenReturn(Optional.of(total));
        when(deviceRepository.findById(10)).thenReturn(Optional.of(cooler));
        DeviceModelSnmpPoint totalPoint = point(101, "TOTAL_WT", "W", "POWER");
        DeviceModelSnmpPoint coolerPoint = point(110, "POWER", "W", "POWER");
        when(deviceModelSnmpPointRepository.findAllEnabledByDeviceModelIds(any()))
                .thenReturn(List.of(totalPoint, coolerPoint));
        when(pointQuery.findLastInRange(anyList(), anyList(), any(), any())).thenReturn(List.of(
                new LastPoint(1, "TOTAL_WT", 100.0, Instant.now().minusSeconds(16 * 60)),
                new LastPoint(10, "POWER", 50.0, Instant.now())
        ));

        PueQueryResponse response = service.getPue(30);

        assertThat(response.value()).isNull();
        assertThat(response.calculationStatus()).isEqualTo("MISSING_DATA");
        assertThat(response.staleDeviceIds()).containsExactly(1);
        assertThat(response.missingDeviceIds()).containsExactly(1);
    }

    private void prepareDevicesAndPowerPoints() {
        Device total1 = device(1, 101);
        Device total2 = device(2, 102);
        Device cooler = device(10, 110);
        when(deviceRepository.findById(1)).thenReturn(Optional.of(total1));
        when(deviceRepository.findById(2)).thenReturn(Optional.of(total2));
        when(deviceRepository.findById(10)).thenReturn(Optional.of(cooler));
        DeviceModelSnmpPoint totalPoint1 = point(101, "TOTAL_WT", "W", "POWER");
        DeviceModelSnmpPoint totalPoint2 = point(102, "T_WT", "W", "POWER");
        DeviceModelSnmpPoint coolerPoint = point(110, "POWER", "W", "POWER");
        when(deviceModelSnmpPointRepository.findAllEnabledByDeviceModelIds(any()))
                .thenReturn(List.of(totalPoint1, totalPoint2, coolerPoint));
    }

    private static PueQueryRequest request() {
        return new PueQueryRequest(
                List.of(new PueSourceRequest(1, "TOTAL_WT"), new PueSourceRequest(2, "T_WT")),
                List.of(new PueSourceRequest(10, "POWER")),
                "last_24h");
    }

    private static Device device(int id, int modelId) {
        Device device = mock(Device.class);
        DeviceModel model = mock(DeviceModel.class);
        lenient().when(device.getId()).thenReturn(id);
        lenient().when(device.getName()).thenReturn("D-" + id);
        lenient().when(device.isEnabled()).thenReturn(true);
        lenient().when(device.getDeviceModel()).thenReturn(model);
        lenient().when(model.getId()).thenReturn(modelId);
        return device;
    }

    private static DeviceModelSnmpPoint point(int modelId, String name, String unit, String type) {
        DeviceModelSnmpPoint point = mock(DeviceModelSnmpPoint.class);
        DeviceModelProtocol protocol = mock(DeviceModelProtocol.class);
        DeviceModel model = mock(DeviceModel.class);
        CommonCode dataPointType = mock(CommonCode.class);
        lenient().when(point.getModelProtocol()).thenReturn(protocol);
        lenient().when(protocol.getDeviceModel()).thenReturn(model);
        lenient().when(model.getId()).thenReturn(modelId);
        lenient().when(point.getName()).thenReturn(name);
        lenient().when(point.getUnit()).thenReturn(unit);
        lenient().when(point.getDataPointType()).thenReturn(dataPointType);
        lenient().when(dataPointType.getCode()).thenReturn(type);
        return point;
    }

    private static PageWidgetPueSource pueSource(Device device, String pointName, PageWidgetPueSourceRole role) {
        PageWidgetPueSource source = mock(PageWidgetPueSource.class);
        when(source.getDevice()).thenReturn(device);
        when(source.getPointName()).thenReturn(pointName);
        when(source.getRole()).thenReturn(role);
        return source;
    }
}
