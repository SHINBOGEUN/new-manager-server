package net.vivans.dcim.module.device.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import net.vivans.dcim.module.mqtt.domain.model.MqttIdentityType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record DeviceRegistrationRequest(
        @NotNull @Valid DeviceCreateRequest device,
        @NotNull Integer protocolTypeId,
        @Valid DeviceProtocolEndpointCreateRequest endpoint,
        @Valid DeviceSnmpInstanceCreateRequest snmpInstance,
        @Valid DeviceEndpointModbusCreateRequest modbus,
        @Valid List<ModbusReading> modbusReadings,
        @Valid LoraIdentity loraIdentity,
        @Valid CollectionGroup collectionGroup
) {
    public record LoraIdentity(@NotNull MqttIdentityType idType, @NotBlank String externalId, Boolean enabled) {
    }

    public record CollectionGroup(@NotNull Integer taskId, @NotNull Integer groupId) {
    }

    /** targetDeviceId를 생략하면 새로 등록되는 장비 자신에게 저장한다. */
    public record ModbusReading(
            @NotNull @Positive Integer pointId,
            @NotNull @Min(0) @Max(247) Integer unitId,
            @NotNull @Min(0) @Max(65535) Integer address,
            Integer targetDeviceId,
            @NotBlank String pointName,
            Boolean enabled
    ) {
    }
}
