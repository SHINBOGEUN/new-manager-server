package net.vivans.dcim.module.device.api;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.shared.api.ApiResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SceneDeviceHistoryController {
    private final JdbcTemplate jdbc;

    @GetMapping("/api/manager/devices/{deviceId}/scene-history")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ApiResponse<?> history(@PathVariable int deviceId) {
        return ApiResponse.ok(jdbc.queryForList("SELECT page_code, previous_host, current_host, actor_name, changed_at FROM scene_device_connection_history WHERE device_id=? ORDER BY id DESC LIMIT 500", deviceId));
    }

    @GetMapping("/api/manager/assets/racks/{code}/history")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ApiResponse<?> rackHistory(@PathVariable String code) {
        return ApiResponse.ok(jdbc.queryForList("SELECT h.action, h.created_dt, d.name AS device_name, h.previous_mount_type, h.previous_rack_location_code, h.previous_rack_side, h.previous_u_position, h.previous_u_height, h.current_mount_type, h.current_rack_location_code, h.current_rack_side, h.current_u_position, h.current_u_height FROM device_rack_placement_history h JOIN devices d ON d.id=h.device_id WHERE h.previous_rack_location_code=? OR h.current_rack_location_code=? ORDER BY h.id DESC LIMIT 500", code, code));
    }
}
