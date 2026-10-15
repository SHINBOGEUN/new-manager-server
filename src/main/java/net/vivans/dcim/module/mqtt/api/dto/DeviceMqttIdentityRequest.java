package net.vivans.dcim.module.mqtt.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import net.vivans.dcim.module.mqtt.domain.model.MqttIdentityType;

/** 기존 장비 등록·수정 요청에 포함되는 현재 MQTT 식별값. */
public record DeviceMqttIdentityRequest(
        @NotNull MqttIdentityType identityType,
        @NotBlank @Size(max = 255) String externalId,
        Boolean enabled
) {}
