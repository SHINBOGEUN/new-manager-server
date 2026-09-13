package net.vivans.dcim.module.pue.api.dto;
import jakarta.validation.Valid; import jakarta.validation.constraints.*; import java.util.List;
public record PueDefinitionRequest(@NotBlank @Size(max=100) String name, String calculationCron, Boolean collectionEnabled,
 List<@Valid PueDefinitionSourceRequest> totalSources, List<@Valid PueDefinitionSourceRequest> coolerSources,
 List<@Valid PueDefinitionDeviceGroupRequest> totalDeviceGroups,
 List<@Valid PueDefinitionDeviceGroupRequest> coolerDeviceGroups) {}
