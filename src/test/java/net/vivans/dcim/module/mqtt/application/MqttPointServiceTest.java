package net.vivans.dcim.module.mqtt.application;

import jakarta.persistence.EntityNotFoundException;
import net.vivans.dcim.module.common.application.CategoryCodeResolver;
import net.vivans.dcim.module.common.application.UnitCodeResolver;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModel;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModelProtocol;
import net.vivans.dcim.module.devicemodel.domain.repository.DeviceModelRepository;
import net.vivans.dcim.module.lora.application.LoraConfigChangedEvent;
import net.vivans.dcim.module.lora.application.LoraDataPointTypeResolver;
import net.vivans.dcim.module.lora.application.LoraValueMapValidator;
import net.vivans.dcim.module.mqtt.api.dto.MqttPointRequest;
import net.vivans.dcim.module.mqtt.domain.model.DeviceModelMqttPoint;
import net.vivans.dcim.module.mqtt.domain.model.MqttPointScope;
import net.vivans.dcim.module.mqtt.domain.repository.DeviceModelMqttPointRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MqttPointServiceTest {
    private final DeviceModelMqttPointRepository points = mock(DeviceModelMqttPointRepository.class);
    private final DeviceModelRepository models = mock(DeviceModelRepository.class);
    private final LoraDataPointTypeResolver types = mock(LoraDataPointTypeResolver.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final DeviceModel model = mock(DeviceModel.class);
    private final MqttPointService service = new MqttPointService(points, models, types,
            mock(UnitCodeResolver.class), mock(CategoryCodeResolver.class), mock(LoraValueMapValidator.class), events);

    @BeforeEach void setup() {
        when(model.getId()).thenReturn(1);
        var protocol = mock(DeviceModelProtocol.class);
        var code = mock(CommonCode.class);
        when(code.getCode()).thenReturn("mqtt");
        when(protocol.getProtocolType()).thenReturn(code);
        when(model.getProtocols()).thenReturn(List.of(protocol));
        when(models.findById(1)).thenReturn(Optional.of(model));
        when(types.resolve(null)).thenReturn(mock(CommonCode.class));
        when(points.findAllByDeviceModelIdOrderByIdAsc(1)).thenReturn(List.of());
        when(points.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    private MqttPointRequest request(MqttPointScope scope) {
        return new MqttPointRequest("object.gpu_temperature", "gpu_temperature", null, null, null,
                1D, null, true, scope, "GPU", "components", "index", "uuid");
    }

    @Test void createsComponentAndNotifiesSensor() {
        var response = service.save(1, null, request(MqttPointScope.COMPONENT));
        assertThat(response.componentScope()).isEqualTo(MqttPointScope.COMPONENT);
        assertThat(response.componentPath()).isEqualTo("components");
        assertThat(response.componentUuidField()).isEqualTo("uuid");
        verify(events).publishEvent(any(LoraConfigChangedEvent.class));
    }

    @Test void deviceScopeClearsComponentConfiguration() {
        var point = DeviceModelMqttPoint.create(model, "object.gpu_temperature", "gpu_temperature",
                mock(CommonCode.class), null, 1D, null, true);
        point.configureComponent("GPU", "components", "index", "uuid");
        when(points.findById(2)).thenReturn(Optional.of(point));
        var response = service.save(1, 2, request(MqttPointScope.DEVICE));
        assertThat(response.componentPath()).isEmpty();
        assertThat(response.componentType()).isNull();
        assertThat(response.componentUuidField()).isNull();
    }

    @Test void duplicateNameIsRejected() {
        var point = DeviceModelMqttPoint.create(model, "other", "gpu_temperature", mock(CommonCode.class), null, 1D, null, true);
        when(points.findAllByDeviceModelIdOrderByIdAsc(1)).thenReturn(List.of(point));
        assertThatThrownBy(() -> service.save(1, null, request(MqttPointScope.COMPONENT)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(points, never()).save(any());
    }

    @Test void missingComponentIndexIsRejected() {
        var r = new MqttPointRequest("object.power", "power", null, null, null, null, null, true,
                MqttPointScope.COMPONENT, "GPU", "components", "", "uuid");
        assertThatThrownBy(() -> service.save(1, null, r)).isInstanceOf(IllegalArgumentException.class);
        verify(points, never()).save(any());
    }

    @Test void anotherModelsPointCannotBeDeleted() {
        var other = mock(DeviceModel.class);
        when(other.getId()).thenReturn(99);
        var point = DeviceModelMqttPoint.create(other, "object.power", "power", mock(CommonCode.class), null, 1D, null, true);
        when(points.findById(2)).thenReturn(Optional.of(point));
        assertThatThrownBy(() -> service.delete(1, 2)).isInstanceOf(EntityNotFoundException.class);
        verify(points, never()).delete(any());
    }
}
