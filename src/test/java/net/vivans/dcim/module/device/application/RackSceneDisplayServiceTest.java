package net.vivans.dcim.module.device.application;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.vivans.dcim.module.device.api.dto.RackLayoutResponse;
import net.vivans.dcim.module.query.domain.PointQuery;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class RackSceneDisplayServiceTest {
    private final JdbcTemplate jdbc=mock(JdbcTemplate.class);
    private final DeviceAssetRackService racks=mock(DeviceAssetRackService.class);
    private final DeviceMeasurementSourceCatalog catalog=mock(DeviceMeasurementSourceCatalog.class);
    private final PointQuery points=mock(PointQuery.class);
    private final RackSceneDisplayService service=new RackSceneDisplayService(jdbc,new ObjectMapper(),racks,catalog,points);
    private final RackSceneDisplayService.Source temp=new RackSceneDisplayService.Source(21,"TEMPERATURE","mqtt");
    private void prepare() {
        var item=mock(RackLayoutResponse.RackLayoutItem.class); when(item.deviceId()).thenReturn(21);
        when(racks.getRackLayout("RACK")).thenReturn(new RackLayoutResponse("RACK","Rack",42,List.of(item)));
        when(catalog.availableSources(Set.of(21))).thenReturn(List.of(new DeviceMeasurementSourceCatalog.Source(21,"온습도",1,"mqtt","TEMPERATURE","°C","TEMPERATURE",21,"MODEL_POINT",false)));
    }
    @Test void acceptsOnlyRackSourcesAndOrderedThresholds() {
        prepare();
        var saved=service.save("RACK",new RackSceneDisplayService.Config(List.of(temp),temp,25.0,30.0));
        assertEquals(25.0,saved.warningC()); assertEquals(temp,saved.temperature());
        verify(jdbc).update(contains("INSERT INTO rack_scene_display_config"),eq("RACK"),anyString());
        assertThrows(IllegalArgumentException.class,()->service.save("RACK",new RackSceneDisplayService.Config(List.of(temp),temp,30.0,25.0)));
        var other=new RackSceneDisplayService.Source(22,"TEMPERATURE","mqtt");
        assertThrows(IllegalArgumentException.class,()->service.save("RACK",new RackSceneDisplayService.Config(List.of(other),null,25.0,30.0)));
    }
    @Test void onlyCelsiusTemperatureCanDriveFloorColor() {
        prepare(); assertTrue(RackSceneDisplayService.isCelsius("°C")); assertTrue(RackSceneDisplayService.isCelsius("CELSIUS"));
        assertFalse(RackSceneDisplayService.isCelsius("°F")); assertFalse(RackSceneDisplayService.isCelsius(null));
        when(catalog.availableSources(Set.of(21))).thenReturn(List.of(new DeviceMeasurementSourceCatalog.Source(21,"온습도",1,"mqtt","TEMPERATURE","°F","TEMPERATURE",21,"MODEL_POINT",false)));
        assertThrows(IllegalArgumentException.class,()->service.save("RACK",new RackSceneDisplayService.Config(List.of(),temp,25.0,30.0)));
    }
    @Test void sumsRequireDistinctRackPowerSourcesAndOldJsonRemainsReadable() throws Exception {
        var old = new ObjectMapper().readValue("{\"labels\":[],\"temperature\":null,\"warningC\":25,\"dangerC\":30}", RackSceneDisplayService.Config.class);
        assertNull(old.sum()); assertEquals(1000.0, RackSceneDisplayService.powerFactor("kW"));
        prepare(); var watts=new RackSceneDisplayService.Source(21,"TOTAL_WT","snmp"); var kilowatts=new RackSceneDisplayService.Source(21,"ACTIVE_POWER","mqtt");
        when(catalog.availableSources(Set.of(21))).thenReturn(List.of(
            new DeviceMeasurementSourceCatalog.Source(21,"PDU",1,"snmp","TOTAL_WT","W","POWER",21,"MODEL_POINT",false),
            new DeviceMeasurementSourceCatalog.Source(21,"PDU",1,"mqtt","ACTIVE_POWER","kW","POWER",21,"MODEL_POINT",false)));
        var config = service.save("RACK",new RackSceneDisplayService.Config(List.of(),null,25.0,30.0,new RackSceneDisplayService.Sum("전력 합계", List.of(watts,kilowatts))));
        assertEquals(2,config.sum().sources().size());
        assertThrows(IllegalArgumentException.class,()->service.save("RACK",new RackSceneDisplayService.Config(List.of(),null,25.0,30.0,new RackSceneDisplayService.Sum("중복",List.of(watts,watts)))));
    }
    @Test void powerSumConvertsUnitsAndNeverAddsPartialData() throws Exception {
        prepare(); var watts=new RackSceneDisplayService.Source(21,"TOTAL_WT","snmp"); var kilowatts=new RackSceneDisplayService.Source(21,"ACTIVE_POWER","mqtt");
        when(catalog.availableSources(Set.of(21))).thenReturn(List.of(
            new DeviceMeasurementSourceCatalog.Source(21,"PDU",1,"snmp","TOTAL_WT","W","POWER",21,"MODEL_POINT",false),
            new DeviceMeasurementSourceCatalog.Source(21,"PDU",1,"mqtt","ACTIVE_POWER","kW","POWER",21,"MODEL_POINT",false)));
        var config=new RackSceneDisplayService.Config(List.of(),null,25.0,30.0,new RackSceneDisplayService.Sum("전력 합계",List.of(watts,kilowatts)));
        when(jdbc.query(anyString(), org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<String>>any(), eq("RACK"))).thenReturn(List.of(new ObjectMapper().writeValueAsString(config)));
        when(points.findLast(eq(List.of(21)),eq(List.of("TOTAL_WT")),any(),eq("snmp"))).thenReturn(List.of(new net.vivans.dcim.module.query.domain.LastPoint(21,"TOTAL_WT",120,java.time.Instant.now())));
        when(points.findLast(eq(List.of(21)),eq(List.of("ACTIVE_POWER")),any(),eq("mqtt"))).thenReturn(List.of(new net.vivans.dcim.module.query.domain.LastPoint(21,"ACTIVE_POWER",1.5,java.time.Instant.now())));
        assertEquals(1620.0,service.values("RACK").sum().value());
        when(points.findLast(eq(List.of(21)),eq(List.of("ACTIVE_POWER")),any(),eq("mqtt"))).thenReturn(List.of());
        assertNull(service.values("RACK").sum().value());
    }
}
