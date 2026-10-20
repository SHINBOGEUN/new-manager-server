package net.vivans.dcim.module.alarm.application;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.alarm.api.AlarmEventResponse;
import net.vivans.dcim.module.alarm.domain.AlarmEvent;
import net.vivans.dcim.module.alarm.domain.ThresholdAlarmEventDetail;
import net.vivans.dcim.module.alarm.infrastructure.AlarmEventRepository;
import net.vivans.dcim.module.alarm.infrastructure.ThresholdAlarmEventDetailRepository;
import net.vivans.dcim.shared.api.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlarmQueryService {
    private final AlarmEventRepository events;
    private final ThresholdAlarmEventDetailRepository thresholdDetails;

    public PageResponse<AlarmEventResponse> list(Authentication auth, int page, int size) {
        if (page < 1 || size < 1 || size > 100) throw new IllegalArgumentException("page/size 값이 잘못되었습니다");
        var pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "raisedDt", "id"));
        return PageResponse.from(isAdmin(auth) ? events.findAll(pageable)
                : events.findAllByVisibilityOrAlarmType("USER_VISIBLE", "THRESHOLD", pageable), this::view);
    }

    public AlarmEventResponse get(Authentication auth, long id) {
        AlarmEvent event = events.findById(id).orElseThrow(() -> new EntityNotFoundException("알람이 없습니다"));
        if (!isAdmin(auth) && !"USER_VISIBLE".equals(event.getVisibility())
                && !"THRESHOLD".equals(event.getAlarmType())) {
            // Do not disclose that an administrator-only alarm exists.
            throw new EntityNotFoundException("알람이 없습니다");
        }
        return view(event);
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
                event.getVisibility(), event.getSourceKey(), event.getSubjectKey(), details,
                event.getRaisedDt(), event.getClearedDt());
    }

    private static AlarmEventResponse.ThresholdDetails thresholdDetails(ThresholdAlarmEventDetail details) {
        return new AlarmEventResponse.ThresholdDetails(details.getThresholdRule().getId(), details.getDevice().getId(),
                details.getDevice().getName(), details.getPointName(), details.getUnit(), details.getOperator(),
                details.getThresholdValue(), details.getMeasuredValue());
    }
}
