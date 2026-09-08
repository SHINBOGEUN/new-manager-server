package net.vivans.dcim.module.pue.api.dto;
import jakarta.validation.Valid; import jakarta.validation.constraints.*; import java.util.List;
public record PueDefinitionRequest(@NotBlank @Size(max=100) String name, String calculationCron, Boolean collectionEnabled,
 @NotEmpty List<@Valid PueDefinitionSourceRequest> totalSources, @NotEmpty List<@Valid PueDefinitionSourceRequest> coolerSources) {}
