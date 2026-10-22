package net.vivans.dcim.module.device.application;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.common.domain.repository.CommonCodeRepository;
import net.vivans.dcim.module.device.api.dto.DeviceMonitoringPolicyRequest;
import net.vivans.dcim.module.device.api.dto.DeviceMonitoringPolicyResponse;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.model.DeviceMonitoringPolicy;
import net.vivans.dcim.module.device.domain.repository.DeviceMonitoringPolicyRepository;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeviceMonitoringPolicyService {
    private static final String MODE_GROUP = "DEVICE_MONITORING_MODE";
    private final DeviceRepository devices;
    private final CommonCodeRepository codes;
    private final DeviceMonitoringPolicyRepository policies;

    @Transactional(readOnly = true)
    public DeviceMonitoringPolicyResponse get(Integer deviceId) {
        requireDevice(deviceId);
        return policies.findByDeviceId(deviceId).map(this::response)
                .orElseGet(() -> response(deviceId, requireMode("PERIODIC"), false));
    }

    @Transactional
    public DeviceMonitoringPolicyResponse save(Integer deviceId, DeviceMonitoringPolicyRequest request) {
        requireDevice(deviceId);
        if (request == null || request.modeCode() == null || request.modeCode().isBlank()) {
            throw new IllegalArgumentException("감시 방식을 선택하세요");
        }
        CommonCode mode = requireMode(request.modeCode());
        DeviceMonitoringPolicy policy = policies.findByDeviceId(deviceId)
                .orElseGet(() -> DeviceMonitoringPolicy.create(deviceId, mode));
        policy.update(deviceId, mode);
        return response(policies.save(policy));
    }

    private Device requireDevice(Integer id) {
        if (id == null || id < 1) throw new IllegalArgumentException("유효하지 않은 장비 ID입니다");
        return devices.findById(id).orElseThrow(() -> new IllegalArgumentException("장비를 찾을 수 없습니다: " + id));
    }

    private CommonCode requireMode(String code) {
        return codes.findByCodeGroupGroupKeyAndCode(MODE_GROUP, code)
                .orElseThrow(() -> new IllegalStateException("DEVICE_MONITORING_MODE 공통코드가 없습니다: " + code));
    }

    private DeviceMonitoringPolicyResponse response(DeviceMonitoringPolicy policy) {
        return response(policy.getDeviceId(), policy.getMonitoringMode(), true);
    }

    private DeviceMonitoringPolicyResponse response(Integer deviceId, CommonCode code, boolean configured) {
        return new DeviceMonitoringPolicyResponse(deviceId, code.getCode(), code.getName(), configured);
    }
}
