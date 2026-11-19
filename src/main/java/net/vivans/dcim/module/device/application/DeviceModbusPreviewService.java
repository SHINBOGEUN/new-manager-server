package net.vivans.dcim.module.device.application;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.collectortask.application.CollectionGroupSpecService;
import net.vivans.dcim.module.collectortask.application.CollectionReadiness;
import net.vivans.dcim.module.collectortask.infrastructure.collector.CollectorJobClient;
import net.vivans.dcim.module.collectortask.infrastructure.collector.CollectorModbusPreviewResponse;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DeviceModbusPreviewService {
    private final DeviceRepository deviceRepository;
    private final CollectionGroupSpecService specService;
    private final CollectorJobClient collectorJobClient;

    @Transactional(readOnly = true)
    public PreviewResult read(Integer deviceId) {
        var device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new EntityNotFoundException("Device not found: " + deviceId));
        CollectionGroupSpecService.ModbusPreviewPlan plan = specService.previewModbus(device);
        if (plan.targets().isEmpty()) {
            return new PreviewResult(List.of(), plan.skipped(), plan.readiness());
        }
        if (plan.targets().size() > 50) {
            throw new IllegalArgumentException("Modbus preview supports at most 50 targets");
        }
        CollectorModbusPreviewResponse response = collectorJobClient.previewModbus(Map.of(
                "targets", plan.targets(), "timeoutMs", 2000, "retries", 1));
        return new PreviewResult(response.targets(), plan.skipped(), plan.readiness());
    }

    public record PreviewResult(List<CollectorModbusPreviewResponse.TargetResult> targets, List<String> skipped,
                                List<CollectionReadiness> readiness) {
    }
}
