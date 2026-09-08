package net.vivans.dcim.module.collectortask.infrastructure.collector;

import java.util.Map;

public record CollectorHealthResponse(String status, int jobs, Map<String, Object> details) {

    public String instanceId() {
        Object value = details == null ? null : details.get("instanceId");
        return value == null ? null : String.valueOf(value);
    }
}
