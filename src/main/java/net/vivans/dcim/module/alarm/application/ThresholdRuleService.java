package net.vivans.dcim.module.alarm.application;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.alarm.api.ThresholdRuleRequest;
import net.vivans.dcim.module.alarm.api.ThresholdRuleResponse;
import net.vivans.dcim.module.alarm.domain.ThresholdRule;
import net.vivans.dcim.module.alarm.infrastructure.AlarmEventRepository;
import net.vivans.dcim.module.alarm.infrastructure.ThresholdRuleRepository;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.common.domain.repository.CommonCodeRepository;
import net.vivans.dcim.module.device.application.DeviceMeasurementSourceCatalog;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ThresholdRuleService {
    private final ThresholdRuleRepository rules;
    private final AlarmEventRepository events;
    private final DeviceRepository devices;
    private final CommonCodeRepository commonCodes;
    private final DeviceMeasurementSourceCatalog sourceCatalog;

    @Transactional(readOnly = true)
    public List<ThresholdRuleResponse> list() {
        List<DeviceMeasurementSourceCatalog.Source> sources = sourceCatalog.availableSources();
        return rules.findAll(Sort.by("id")).stream().map(rule -> view(rule, sources)).toList();
    }

    @Transactional(readOnly = true)
    public ThresholdRuleResponse get(long id) {
        ThresholdRule rule = requireRule(id);
        return view(rule, sourceCatalog.availableSources(java.util.Set.of(rule.getDevice().getId())));
    }

    @Transactional
    public ThresholdRuleResponse create(ThresholdRuleRequest request) {
        validateThresholdLevels(request);
        Resolved resolved = resolve(request);
        if (rules.existsByDevice_IdAndPointName(resolved.device().getId(), resolved.source().pointName())) {
            throw new IllegalArgumentException("이 장비의 측정항목에는 이미 임계값이 등록되어 있습니다. 기존 설정을 수정하세요.");
        }
        ThresholdRule saved = rules.save(new ThresholdRule(resolved.device(), resolved.protocolType(),
                resolved.source().pointName(), request.operator(), request.warningThresholdValue(),
                request.criticalThresholdValue(), enabled(request)));
        return view(saved, List.of(resolved.source()));
    }

    @Transactional
    public ThresholdRuleResponse update(long id, ThresholdRuleRequest request) {
        validateThresholdLevels(request);
        ThresholdRule rule = requireRule(id);
        Resolved resolved = resolve(request);
        if (rules.existsByDevice_IdAndPointNameAndIdNot(resolved.device().getId(),
                resolved.source().pointName(), id)) {
            throw new IllegalArgumentException("이 장비의 측정항목에는 이미 임계값이 등록되어 있습니다.");
        }
        rule.change(resolved.device(), resolved.protocolType(), resolved.source().pointName(),
                request.operator(), request.warningThresholdValue(), request.criticalThresholdValue(), enabled(request));
        closeOpenEvents(id);
        return view(rule, List.of(resolved.source()));
    }

    @Transactional
    public ThresholdRuleResponse disable(long id) {
        ThresholdRule rule = requireRule(id);
        rule.disable();
        closeOpenEvents(id);
        return view(rule, sourceCatalog.availableSources(java.util.Set.of(rule.getDevice().getId())));
    }

    private Resolved resolve(ThresholdRuleRequest request) {
        Device device = devices.findById(request.deviceId()).orElseThrow(() ->
                new EntityNotFoundException("장비가 없습니다: " + request.deviceId()));
        if (!device.isEnabled()) throw new IllegalArgumentException("비활성 장비에는 임계값을 설정할 수 없습니다");
        String protocol = request.protocol().trim().toLowerCase();
        DeviceMeasurementSourceCatalog.Source source = sourceCatalog.availableSources(java.util.Set.of(device.getId()))
                .stream().filter(item -> item.protocol().equalsIgnoreCase(protocol)
                        && item.pointName().equals(request.pointName().trim()))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("장비에서 수집 가능한 측정항목이 아닙니다"));
        if (!source.thresholdSupported()) {
            throw new IllegalArgumentException("공통코드에서 임계값 사용이 허용되지 않은 측정 유형입니다");
        }
        if (source.dataPointTypeId() == null || source.unit() == null || source.unit().isBlank()) {
            throw new IllegalArgumentException("임계값에는 데이터 유형과 단위가 등록된 측정항목만 사용할 수 있습니다");
        }
        CommonCode protocolType = commonCodes.findByCodeGroupGroupKeyAndCode("PROTOCOL_TYPE", protocol)
                .orElseThrow(() -> new IllegalArgumentException("등록된 프로토콜 코드가 아닙니다: " + protocol));
        CommonCode dataPointType = commonCodes.findById(source.dataPointTypeId()).orElseThrow(() ->
                new EntityNotFoundException("측정 유형 코드가 없습니다: " + source.dataPointTypeId()));
        if (!"DATA_POINT_TYPE".equals(dataPointType.getCodeGroup().getGroupKey())
                || !dataPointType.isThresholdSupported()) {
            throw new IllegalArgumentException("임계값 사용이 허용되지 않은 데이터 유형입니다");
        }
        return new Resolved(device, protocolType, source);
    }

    private ThresholdRuleResponse view(ThresholdRule rule, List<DeviceMeasurementSourceCatalog.Source> sources) {
        DeviceMeasurementSourceCatalog.Source source = sources.stream()
                .filter(item -> item.protocol().equalsIgnoreCase(rule.getProtocolType().getCode())
                        && item.pointName().equals(rule.getPointName()))
                .findFirst().orElse(null);
        return new ThresholdRuleResponse(rule.getId(), rule.getDevice().getId(), rule.getDevice().getName(),
                rule.getDevice().getDeviceModel().getName(), rule.getProtocolType().getCode(), rule.getPointName(),
                source == null ? null : source.dataPointType(), source == null ? null : source.unit(),
                rule.getOperator(), rule.getWarningThresholdValue(), rule.getCriticalThresholdValue(),
                rule.isEnabled(), rule.getCreatedDt(), rule.getUpdatedDt());
    }

    private static void validateThresholdLevels(ThresholdRuleRequest request) {
        var warning = request.warningThresholdValue();
        var critical = request.criticalThresholdValue();
        if (warning == null && critical == null) {
            throw new IllegalArgumentException("주의 또는 위험 기준값을 하나 이상 입력하세요.");
        }
        if (warning != null && critical != null) {
            int comparison = critical.compareTo(warning);
            if ("ABOVE".equals(request.operator()) && comparison <= 0) {
                throw new IllegalArgumentException("초과 조건에서는 위험 기준값이 주의 기준값보다 커야 합니다.");
            }
            if ("BELOW".equals(request.operator()) && comparison >= 0) {
                throw new IllegalArgumentException("미만 조건에서는 위험 기준값이 주의 기준값보다 작아야 합니다.");
            }
        }
    }

    private void closeOpenEvents(long id) {
        events.findOpenThresholdEvents(id)
                .forEach(event -> event.clear(Instant.now()));
    }

    private static boolean enabled(ThresholdRuleRequest request) {
        return request.enabled() == null || request.enabled();
    }

    private ThresholdRule requireRule(long id) {
        return rules.findById(id).orElseThrow(() -> new EntityNotFoundException("임계값 설정이 없습니다: " + id));
    }

    private record Resolved(Device device, CommonCode protocolType,
                            DeviceMeasurementSourceCatalog.Source source) {}
}
