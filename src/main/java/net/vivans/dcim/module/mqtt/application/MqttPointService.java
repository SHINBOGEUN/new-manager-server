package net.vivans.dcim.module.mqtt.application;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.common.application.CategoryCodeResolver;
import net.vivans.dcim.module.common.application.UnitCodeResolver;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModel;
import net.vivans.dcim.module.devicemodel.domain.repository.DeviceModelRepository;
import net.vivans.dcim.module.lora.application.LoraConfigChangedEvent;
import net.vivans.dcim.module.lora.application.LoraDataPointTypeResolver;
import net.vivans.dcim.module.lora.application.LoraValueMapValidator;
import net.vivans.dcim.module.mqtt.api.dto.MqttPointRequest;
import net.vivans.dcim.module.mqtt.api.dto.MqttPointResponse;
import net.vivans.dcim.module.mqtt.domain.model.DeviceModelMqttPoint;
import net.vivans.dcim.module.mqtt.domain.model.MqttPointScope;
import net.vivans.dcim.module.mqtt.domain.repository.DeviceModelMqttPointRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MqttPointService {
    private final DeviceModelMqttPointRepository points;
    private final DeviceModelRepository models;
    private final LoraDataPointTypeResolver types;
    private final UnitCodeResolver units;
    private final CategoryCodeResolver categories;
    private final LoraValueMapValidator valueMaps;
    private final ApplicationEventPublisher events;

    public List<MqttPointResponse> list(Integer modelId) {
        model(modelId);
        return points.findAllByDeviceModelIdOrderByIdAsc(modelId).stream().map(MqttPointResponse::from).toList();
    }

    @Transactional
    public MqttPointResponse save(Integer modelId, Integer id, MqttPointRequest r) {
        DeviceModel model = model(modelId);
        if (model.getProtocols().stream().noneMatch(p -> "MQTT".equalsIgnoreCase(p.getProtocolType().getCode()))
                && !"LORA_SENSOR".equals(model.getDeviceType().getCode())) {
            throw new IllegalArgumentException("MQTT 지원 모델을 선택하세요.");
        }
        valueMaps.validate(r.valueMap());
        if (r.scale() != null && !Double.isFinite(r.scale())) throw new IllegalArgumentException("scale must be finite");
        String path = r.componentScope() == MqttPointScope.COMPONENT && r.componentPath() != null
                ? r.componentPath().trim() : "";
        for (var other : points.findAllByDeviceModelIdOrderByIdAsc(modelId)) {
            if (id != null && Objects.equals(other.getId(), id)) continue;
            if (other.getPointName().equals(r.pointName().trim())
                    || (other.getComponentPath().equals(path) && other.getPayloadField().equals(r.payloadField().trim()))) {
                throw new IllegalArgumentException("같은 포인트 이름 또는 같은 범위의 값 경로가 이미 존재합니다.");
            }
        }
        DeviceModelMqttPoint point;
        if (id == null) {
            point = DeviceModelMqttPoint.create(model, r.payloadField(), r.pointName(), types.resolve(r.dataPointTypeId()),
                    units.resolve(r.unitCodeId()), r.scale(), r.valueMap(), !Boolean.FALSE.equals(r.enabled()), categories.resolve(r.categoryCodeId()));
        } else {
            point = point(modelId, id);
            point.update(r.payloadField(), r.pointName(), types.resolve(r.dataPointTypeId()), units.resolve(r.unitCodeId()),
                    r.scale(), r.valueMap(), !Boolean.FALSE.equals(r.enabled()), categories.resolve(r.categoryCodeId()));
        }
        if (r.componentScope() == MqttPointScope.COMPONENT) {
            point.configureComponent(r.componentType(), r.componentPath(), r.componentIndexField(), r.componentUuidField());
        } else {
            point.configureDevice();
        }
        var saved = points.save(point);
        events.publishEvent(new LoraConfigChangedEvent("mqtt-point-saved:" + saved.getId()));
        return MqttPointResponse.from(saved);
    }

    @Transactional
    public Integer delete(Integer modelId, Integer id) {
        points.delete(point(modelId, id));
        events.publishEvent(new LoraConfigChangedEvent("mqtt-point-deleted:" + id));
        return id;
    }

    private DeviceModel model(Integer id) {
        return models.findById(id).orElseThrow(() -> new EntityNotFoundException("DeviceModel not found: " + id));
    }
    private DeviceModelMqttPoint point(Integer modelId, Integer id) {
        var point = points.findById(id).orElseThrow(() -> new EntityNotFoundException("MQTT point not found: " + id));
        if (!point.getDeviceModel().getId().equals(modelId)) throw new EntityNotFoundException("MQTT point not found: " + id);
        return point;
    }
}
