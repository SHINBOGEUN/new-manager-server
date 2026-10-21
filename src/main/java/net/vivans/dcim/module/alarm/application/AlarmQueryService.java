package net.vivans.dcim.module.alarm.application;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.alarm.api.AlarmEventResponse;
import net.vivans.dcim.module.alarm.domain.AlarmEvent;
import net.vivans.dcim.module.alarm.domain.ThresholdAlarmEventDetail;
import net.vivans.dcim.module.alarm.infrastructure.AlarmEventRepository;
import net.vivans.dcim.module.alarm.infrastructure.ThresholdAlarmEventDetailRepository;
import net.vivans.dcim.module.location.domain.model.LocationNode;
import net.vivans.dcim.module.location.domain.repository.LocationNodeRepository;
import net.vivans.dcim.shared.api.PageResponse;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import java.util.List;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlarmQueryService {
    private final AlarmEventRepository events;
    private final ThresholdAlarmEventDetailRepository thresholdDetails;
    private final LocationNodeRepository locationNodes;

    public PageResponse<AlarmEventResponse> list(Authentication auth, int page, int size,
            LocalDate from, LocalDate to, String locationCode, Integer modelId, Integer deviceId,
            String severity, String pointName) {
        if (page < 1 || size < 1 || size > 100) throw new IllegalArgumentException("page/size 값이 잘못되었습니다");
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("시작 날짜는 종료 날짜보다 늦을 수 없습니다");
        }
        var pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "raisedDt", "id"));
        var specification = alarmFilters(auth, from, to, locationCode, modelId, deviceId, severity, pointName);
        return PageResponse.from(events.findAll(specification, pageable), this::view);
    }

    private Specification<AlarmEvent> alarmFilters(Authentication auth, LocalDate from, LocalDate to,
            String locationCode, Integer modelId, Integer deviceId, String severity, String pointName) {
        Instant fromInstant = from == null ? null : from.atStartOfDay(KOREA).toInstant();
        Instant toExclusive = to == null ? null : to.plusDays(1).atStartOfDay(KOREA).toInstant();
        Set<String> locationCodes = locationCode == null || locationCode.isBlank()
                ? Set.of() : resolveLocationSubtree(locationCode.trim());
        String normalizedSeverity = blankToNull(severity);
        String normalizedPoint = blankToNull(pointName);
        String visibleToUser = isAdmin(auth) ? null : "USER_VISIBLE";

        return (root, query, builder) -> {
            var detail = root.join("thresholdDetail", JoinType.LEFT);
            var device = detail.join("device", JoinType.LEFT);
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (visibleToUser != null) predicates.add(builder.equal(root.get("visibility"), visibleToUser));
            if (fromInstant != null) predicates.add(builder.greaterThanOrEqualTo(root.get("raisedDt"), fromInstant));
            if (toExclusive != null) predicates.add(builder.lessThan(root.get("raisedDt"), toExclusive));
            if (!locationCodes.isEmpty()) predicates.add(device.get("locationNode").get("code").in(locationCodes));
            if (modelId != null) predicates.add(builder.equal(device.get("deviceModel").get("id"), modelId));
            if (deviceId != null) predicates.add(builder.equal(device.get("id"), deviceId));
            if (normalizedSeverity != null) predicates.add(builder.equal(root.get("severity"), normalizedSeverity));
            if (normalizedPoint != null) predicates.add(builder.like(builder.lower(detail.get("pointName")),
                    "%" + normalizedPoint.toLowerCase(java.util.Locale.ROOT) + "%"));
            return builder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private Set<String> resolveLocationSubtree(String locationCode) {
        LocationNode root = locationNodes.findByCode(locationCode)
                .orElseThrow(() -> new EntityNotFoundException("위치를 찾을 수 없습니다: " + locationCode));
        Map<String, List<String>> children = new HashMap<>();
        for (LocationNode node : locationNodes.findAll()) {
            if (node.getParent() != null) {
                children.computeIfAbsent(node.getParent().getCode(), ignored -> new ArrayList<>()).add(node.getCode());
            }
        }
        Set<String> result = new HashSet<>();
        ArrayDeque<String> pending = new ArrayDeque<>();
        pending.add(root.getCode());
        while (!pending.isEmpty()) {
            String code = pending.removeFirst();
            if (result.add(code)) pending.addAll(children.getOrDefault(code, List.of()));
        }
        return result;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");

    public AlarmEventResponse get(Authentication auth, long id) {
        AlarmEvent event = events.findById(id).orElseThrow(() -> new EntityNotFoundException("알람이 없습니다"));
        if (!isAdmin(auth) && !"USER_VISIBLE".equals(event.getVisibility())) {
            // Do not disclose that an administrator-only alarm exists.
            throw new EntityNotFoundException("알람이 없습니다");
        }
        return view(event);
    }

    public long latestId(Authentication auth) {
        requireReader(auth);
        return isAdmin(auth) ? events.findLatestId() : events.findLatestVisibleId();
    }

    public List<AlarmEventResponse> updates(Authentication auth, long afterId, int limit) {
        requireReader(auth);
        if (afterId < 0 || limit < 1 || limit > 100) throw new IllegalArgumentException("afterId/limit 값이 잘못되었습니다");
        var page = PageRequest.of(0, limit);
        return (isAdmin(auth) ? events.findByIdGreaterThanOrderByIdAsc(afterId, page)
                : events.findVisibleAfter(afterId, page)).stream().map(this::view).toList();
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public AlarmEventResponse forNotification(long id) {
        return events.findById(id).map(this::view).orElse(null);
    }

    public void requireReader(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth.getAuthorities().stream().noneMatch(authority ->
                "ROLE_ADMIN".equals(authority.getAuthority()) || "ROLE_USER".equals(authority.getAuthority()))) {
            throw new AccessDeniedException("알람 조회 권한이 없습니다");
        }
    }

    private static boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private AlarmEventResponse view(AlarmEvent event) {
        var details = thresholdDetails.findById(event.getId()).map(AlarmQueryService::thresholdDetails).orElse(null);
        return new AlarmEventResponse(event.getId(), event.getAlarmType(), event.getSeverity(),
                event.getVisibility(), details, event.getRaisedDt(), event.getClearedDt());
    }

    private static AlarmEventResponse.ThresholdDetails thresholdDetails(ThresholdAlarmEventDetail details) {
        return new AlarmEventResponse.ThresholdDetails(details.getThresholdRule().getId(), details.getDevice().getId(),
                details.getDevice().getName(), details.getPointName(), details.getUnit(), details.getOperator(),
                details.getThresholdValue(), details.getMeasuredValue());
    }
}
