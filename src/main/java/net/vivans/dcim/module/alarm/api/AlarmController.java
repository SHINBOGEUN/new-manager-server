package net.vivans.dcim.module.alarm.api;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.alarm.application.AlarmQueryService;
import net.vivans.dcim.shared.api.ApiResponse;
import net.vivans.dcim.shared.api.PageResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager/alarms")
@PreAuthorize("hasAnyRole('ADMIN','USER')")
public class AlarmController {
    private final AlarmQueryService service;

    @GetMapping public ApiResponse<PageResponse<AlarmEventResponse>> list(Authentication auth,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        service.requireReader(auth);
        return ApiResponse.ok(service.list(auth, page, size));
    }
    @GetMapping("/{id}") public ApiResponse<AlarmEventResponse> get(Authentication auth, @PathVariable long id) {
        service.requireReader(auth);
        return ApiResponse.ok(service.get(auth, id));
    }
}
