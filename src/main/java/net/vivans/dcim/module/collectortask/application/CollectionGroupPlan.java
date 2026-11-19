package net.vivans.dcim.module.collectortask.application;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Collector에 보낼 본문과 운영 화면에서 사용할 수집원별 저장 대상의 대응 관계. */
public record CollectionGroupPlan(
        Object payload,
        List<String> skipped,
        Map<Integer, List<PointSource>> pointsBySourceDeviceId,
        List<SkippedReason> reasons
) {
    public CollectionGroupPlan {
        skipped = List.copyOf(skipped);
        reasons = List.copyOf(reasons);
        Map<Integer, List<PointSource>> copied = new LinkedHashMap<>();
        pointsBySourceDeviceId.forEach((id, points) -> copied.put(id, List.copyOf(points)));
        pointsBySourceDeviceId = Map.copyOf(copied);
    }

    /** 기존 호출부 소스 호환. 자유 문자열에서 원인을 역으로 파싱하지 않는다. */
    public CollectionGroupPlan(Object payload, List<String> skipped,
                               Map<Integer, List<PointSource>> pointsBySourceDeviceId) {
        this(payload, skipped, pointsBySourceDeviceId, List.of());
    }

    public List<SkippedReason> reasonsForDevice(Integer deviceId) {
        return reasons.stream().filter(reason -> deviceId.equals(reason.deviceId())).toList();
    }

    public List<CollectionReadiness> readinessForDevice(Integer deviceId) {
        return reasons.stream().filter(reason -> reason.deviceId() == null || deviceId.equals(reason.deviceId()))
                .map(SkippedReason::readiness).distinct().toList();
    }

    /** 장비별/공통 범위를 별도로 보존하고 기존 skipped 문구는 표시용으로만 쓴다. */
    public record SkippedReason(Integer deviceId, CollectionReadiness readiness, String message, Kind kind) {
        public enum Kind { SETTING, MODEL_MISMATCH, UNSUPPORTED }
    }

    public List<PointSource> pointsForSource(Integer sourceDeviceId) {
        return pointsBySourceDeviceId.getOrDefault(sourceDeviceId, List.of());
    }

    public boolean includesSource(Integer sourceDeviceId) {
        return !pointsForSource(sourceDeviceId).isEmpty();
    }

    public Set<String> pointNamesForSource(Integer sourceDeviceId) {
        Set<String> names = new LinkedHashSet<>();
        for (PointSource point : pointsForSource(sourceDeviceId)) {
            names.add(point.pointName());
        }
        return names;
    }

    public Map<String, String> unitsForSource(Integer sourceDeviceId) {
        Map<String, String> units = new LinkedHashMap<>();
        for (PointSource point : pointsForSource(sourceDeviceId)) {
            if (point.unit() != null) {
                units.put(point.pointName(), point.unit());
            }
        }
        return units;
    }

    public record PointSource(Integer storageDeviceId, String pointName, String unit) {
    }
}
