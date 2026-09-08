package net.vivans.dcim.module.pue.api.dto;
import net.vivans.dcim.module.pue.domain.model.*; import java.util.List;
public record PueDefinitionResponse(Integer id,String name,String calculationCron,boolean collectionEnabled,int configVersion,String collectorJobId,List<Source> sources) {
 public record Source(Integer deviceId,String deviceName,String role,String pointName) {}
 public static PueDefinitionResponse from(PueDefinition d){return new PueDefinitionResponse(d.getId(),d.getName(),d.getCalculationCron(),d.isCollectionEnabled(),d.getConfigVersion(),d.getCollectorJobId(),d.getSources().stream().map(s->new Source(s.getDevice().getId(),s.getDevice().getName(),s.getRole().name(),s.getPointName())).toList());}
}
