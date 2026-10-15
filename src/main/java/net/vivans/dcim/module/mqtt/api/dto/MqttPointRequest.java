package net.vivans.dcim.module.mqtt.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import net.vivans.dcim.module.mqtt.domain.model.MqttPointScope;

public record MqttPointRequest(
        @NotBlank @Size(max = 255) String payloadField,
        @NotBlank @Size(max = 255) String pointName,
        Integer dataPointTypeId, Integer categoryCodeId, Integer unitCodeId,
        Double scale, @Size(max = 1000) String valueMap, Boolean enabled,
        MqttPointScope componentScope,
        @Size(max = 50) String componentType,
        @Size(max = 255) String componentPath,
        @Size(max = 255) String componentIndexField,
        @Size(max = 255) String componentUuidField
) {}
