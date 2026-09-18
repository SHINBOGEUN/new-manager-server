package net.vivans.dcim.module.collectortask.api.dto;

import net.vivans.dcim.module.collectortask.application.CollectionGroupSpec;
import net.vivans.dcim.module.collectortask.application.CollectionGroupSpecService;
import net.vivans.dcim.module.collectortask.domain.model.CollectionTaskDevice;
import net.vivans.dcim.module.collectortask.domain.model.CollectionTaskGroup;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public record CollectionTaskGroupResponse(
        Integer id,
        String name,
        String cronExpression,
        String generatedSpec,
        String collectorJobId,
        boolean active,
        List<CollectionTaskDeviceResponse> devices,
        Instant createdDt,
        Instant updatedDt
) {

    /**
     * 저장된(캐시) generatedSpec이 아니라, 조회 시점에 spec을 새로 계산해서 넘겨준다.
     * {@link CollectionTaskDeviceResponse#excluded}가 항상 "지금 이 순간" 기준으로
     * 맞아야 하기 때문 (수집 상태 화면과 동일한 방식).
     */
    public static CollectionTaskGroupResponse from(CollectionTaskGroup group, CollectionGroupSpecService specService) {
        CollectionGroupSpec spec = specService.generate(group);
        List<CollectionTaskDeviceResponse> devices = new ArrayList<>();
        for (CollectionTaskDevice mapping : group.getDevices()) {
            devices.add(CollectionTaskDeviceResponse.from(mapping, spec));
        }
        return new CollectionTaskGroupResponse(
                group.getId(),
                group.getName(),
                group.getCronExpression(),
                group.getGeneratedSpec(),
                group.getCollectorJobId(),
                group.isActive(),
                devices,
                group.getCreatedDt(),
                group.getUpdatedDt()
        );
    }
}
