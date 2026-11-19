package net.vivans.dcim.module.collectortask.api.dto;

import net.vivans.dcim.module.collectortask.application.CollectionGroupPlan;
import net.vivans.dcim.module.collectortask.application.CollectionReadiness;
import java.util.List;
import net.vivans.dcim.module.collectortask.domain.model.CollectionTaskDevice;

/**
 * 그룹-장비 연결(DB) 기준의 응답. {@code excluded}는 "연결은 있지만 지금 이 그룹의
 * 실제 수집 대상(spec)에는 없다"는 뜻으로, 모델 불일치·비활성·통신 설정 누락 등
 * 프로토콜과 무관한 모든 제외 사유를 함께 표현한다.
 */
public record CollectionTaskDeviceResponse(
        Integer deviceId,
        String deviceName,
        Integer modelId,
        String modelName,
        boolean excluded,
        String exclusionReason,
        List<CollectionReadiness> readiness
) {

    public static CollectionTaskDeviceResponse from(CollectionTaskDevice mapping, CollectionGroupPlan spec) {
        Integer deviceId = mapping.getDevice().getId();
        boolean inTargets = spec != null && spec.includesSource(deviceId);
        String reason = null;
        if (!inTargets) {
            reason = spec == null ? null
                    : spec.reasonsForDevice(deviceId).stream()
                        .map(CollectionGroupPlan.SkippedReason::message)
                        .findFirst()
                        .orElseGet(() -> spec.pointsForSource(deviceId).isEmpty()
                                ? "이 수집 작업에 유효한 측정 항목이 없습니다."
                                : null);
        }
        return new CollectionTaskDeviceResponse(
                deviceId,
                mapping.getDevice().getName(),
                mapping.getDevice().getDeviceModel().getId(),
                mapping.getDevice().getDeviceModel().getName(),
                !inTargets,
                reason,
                spec == null ? List.of() : spec.readinessForDevice(deviceId)
        );
    }
}
