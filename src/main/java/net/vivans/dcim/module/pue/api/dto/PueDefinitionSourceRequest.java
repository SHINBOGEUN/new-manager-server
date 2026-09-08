package net.vivans.dcim.module.pue.api.dto;
import jakarta.validation.constraints.*;
public record PueDefinitionSourceRequest(@NotNull Integer deviceId, @NotBlank @Size(max=100) String pointName) {}
