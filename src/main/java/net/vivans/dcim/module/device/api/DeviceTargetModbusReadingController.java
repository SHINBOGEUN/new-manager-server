package net.vivans.dcim.module.device.api;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.application.DeviceModbusReadingQueryService;
import net.vivans.dcim.shared.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager/devices/{deviceId}/modbus-readings")
public class DeviceTargetModbusReadingController {
    private final DeviceModbusReadingQueryService service;

    @GetMapping
    public ApiResponse<List<DeviceModbusReadingQueryService.TargetReading>> getTargetReadings(@PathVariable Integer deviceId) {
        return ApiResponse.ok(service.getTargetReadings(deviceId));
    }
}
