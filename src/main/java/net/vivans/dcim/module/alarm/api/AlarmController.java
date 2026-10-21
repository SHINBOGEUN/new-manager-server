package net.vivans.dcim.module.alarm.api;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.alarm.application.AlarmQueryService;
import net.vivans.dcim.shared.api.ApiResponse;
import net.vivans.dcim.shared.api.PageResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager/alarms")
@PreAuthorize("hasAnyRole('ADMIN','USER')")
public class AlarmController {
    private final AlarmQueryService service;
    private final net.vivans.dcim.module.alarm.infrastructure.AlarmSocketTickets tickets;

    @GetMapping("/latest-id")
    public ApiResponse<Map<String, Long>> latestId(Authentication auth) {
        return ApiResponse.ok(Map.of("id", service.latestId(auth)));
    }

    @GetMapping("/updates")
    public ApiResponse<List<AlarmEventResponse>> updates(Authentication auth,
            @RequestParam(defaultValue = "0") long afterId, @RequestParam(defaultValue = "100") int limit) {
        return ApiResponse.ok(service.updates(auth, afterId, limit));
    }

    @PostMapping("/socket-ticket")
    public ApiResponse<Map<String, String>> socketTicket(Authentication auth) {
        service.requireReader(auth);
        return ApiResponse.ok(Map.of("ticket", tickets.issue(auth)));
    }

    @GetMapping
    public ApiResponse<PageResponse<AlarmEventResponse>> list(Authentication auth,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "locationCode", required = false) String locationCode,
            @RequestParam(name = "modelId", required = false) Integer modelId,
            @RequestParam(name = "deviceId", required = false) Integer deviceId,
            @RequestParam(name = "severity", required = false) String severity,
            @RequestParam(name = "pointName", required = false) String pointName) {
        service.requireReader(auth);
        return ApiResponse.ok(service.list(auth, page, size, from, to, locationCode, modelId, deviceId, severity, pointName));
    }
    @GetMapping("/{id}") public ApiResponse<AlarmEventResponse> get(Authentication auth, @PathVariable long id) {
        service.requireReader(auth);
        return ApiResponse.ok(service.get(auth, id));
    }
}
