package net.vivans.dcim.module.device.application;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RackWidgetValidationTest {
    private final DeviceMeasurementSourceCatalog.Source available = new DeviceMeasurementSourceCatalog.Source(8, "PDU", 1, "snmp", "WATT", "W", null, 8, "MODEL_POINT", false);
    private RackWidgetService.Widget widget(RackWidgetService.Source source) {
        return new RackWidgetService.Widget("전력", "VALUE", 1, List.of(source));
    }
    @Test void allowsOnlyAnAvailableSourceOfTheRack() {
        assertDoesNotThrow(() -> RackWidgetService.validateWidgets(List.of(widget(new RackWidgetService.Source(8,"WATT","snmp"))), List.of(available)));
        assertThrows(IllegalArgumentException.class, () -> RackWidgetService.validateWidgets(List.of(widget(new RackWidgetService.Source(9,"WATT","snmp"))), List.of(available)));
        assertThrows(IllegalArgumentException.class, () -> RackWidgetService.validateWidgets(List.of(widget(new RackWidgetService.Source(8,"WATT","mqtt"))), List.of(available)));
    }
    @Test void rejectsMalformedAndAmbiguousWidgets() {
        var ambiguous = new DeviceMeasurementSourceCatalog.Source(8, "PDU", 1, "snmp", "WATT", "W", null, 8, "MODEL_POINT", true);
        assertThrows(IllegalArgumentException.class, () -> RackWidgetService.validateWidgets(List.of(widget(new RackWidgetService.Source(8,"WATT","snmp"))), List.of(ambiguous)));
        assertThrows(IllegalArgumentException.class, () -> RackWidgetService.validateWidgets(List.of(new RackWidgetService.Widget("test", null, 1, List.of())), List.of(available)));
        assertDoesNotThrow(() -> RackWidgetService.validateWidgets(List.of(), List.of()));
    }
    @Test void validatesPowerSourcesAndIntervals() {
        var other = new DeviceMeasurementSourceCatalog.Source(9, "PDU B", 1, "snmp", "WATT", "W", null, 9, "MODEL_POINT", false);
        var a = new RackWidgetService.Source(8, "WATT", "snmp");
        var b = new RackWidgetService.Source(9, "WATT", "snmp");
        var valid = List.of(available, other);
        assertDoesNotThrow(() -> RackWidgetService.validateWidgets(List.of(new RackWidgetService.Widget("전력", "POWER_TREND", 2, List.of(a,b), 24, 15)), valid));
        assertThrows(IllegalArgumentException.class, () -> RackWidgetService.validateWidgets(List.of(new RackWidgetService.Widget("전력", "POWER_TREND", 2, List.of(a,a), 24, 15)), valid));
        assertThrows(IllegalArgumentException.class, () -> RackWidgetService.validateWidgets(List.of(new RackWidgetService.Widget("전력", "POWER_TREND", 2, List.of(a,b), 999, 15)), valid));
        assertThrows(IllegalArgumentException.class, () -> RackWidgetService.validateWidgets(List.of(new RackWidgetService.Widget("상별", "PHASE_BALANCE", 2, List.of(a,b))), valid));
    }
    @Test void readsPreviouslySavedWidgetsWithoutTrendOptions() throws Exception {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var widget = mapper.readValue("{\"title\":\"전력\",\"type\":\"VALUE\",\"width\":1,\"sources\":[{\"deviceId\":8,\"pointName\":\"WATT\",\"protocol\":\"snmp\"}]}", RackWidgetService.Widget.class);
        assertNull(widget.rangeHours());
        assertDoesNotThrow(() -> RackWidgetService.validateWidgets(List.of(widget), List.of(available)));
    }
    @Test void phaseBalanceRequiresThreeCurrentPointsFromEachPdu() {
        var definitions = new java.util.ArrayList<DeviceMeasurementSourceCatalog.Source>();
        var chosen = new java.util.ArrayList<RackWidgetService.Source>();
        for (int device : List.of(8, 9)) for (String point : List.of("L1_AMP", "L2_AMP", "L3_AMP")) {
            definitions.add(new DeviceMeasurementSourceCatalog.Source(device, "PDU", 1, "snmp", point, "A", null, device, "MODEL_POINT", false));
            chosen.add(new RackWidgetService.Source(device, point, "snmp"));
        }
        assertDoesNotThrow(() -> RackWidgetService.validateWidgets(List.of(new RackWidgetService.Widget("상별", "PHASE_BALANCE", 2, chosen)), definitions));
        assertDoesNotThrow(() -> RackWidgetService.validateWidgets(List.of(new RackWidgetService.Widget("상별", "PHASE_BALANCE", 2, chosen.subList(0, 3))), definitions));
        java.util.Collections.swap(chosen, 0, 3);
        assertThrows(IllegalArgumentException.class, () -> RackWidgetService.validateWidgets(List.of(new RackWidgetService.Widget("상별", "PHASE_BALANCE", 2, chosen)), definitions));
    }
    @Test void explicitWidgetPositionsMustFitAndNotOverlap() {
        var source = List.of(new RackWidgetService.Source(8,"WATT","snmp"));
        var left = new RackWidgetService.Widget("왼쪽", "VALUE", 1, source, null, null, 0, 1);
        var right = new RackWidgetService.Widget("오른쪽", "PIE", 1, source, null, null, 1, 1);
        var overlap = new RackWidgetService.Widget("겹침", "TREND", 2, source, null, null, 0, 1);
        var outside = new RackWidgetService.Widget("범위", "VALUE", 2, source, null, null, 1, 0);
        assertDoesNotThrow(() -> RackWidgetService.validateWidgets(List.of(left,right), List.of(available)));
        assertThrows(IllegalArgumentException.class, () -> RackWidgetService.validateWidgets(List.of(left,overlap), List.of(available)));
        assertThrows(IllegalArgumentException.class, () -> RackWidgetService.validateWidgets(List.of(outside), List.of(available)));
    }
    @Test void customSizeAndPositionAreValidatedInTwoDimensions() throws Exception {
        var source = List.of(new RackWidgetService.Source(8,"WATT","snmp"));
        var first = new RackWidgetService.Widget("A", "VALUE", 1, source, null, null, null, null, 0, 0, 4, 5);
        var beside = new RackWidgetService.Widget("B", "PIE", 1, source, null, null, null, null, 4, 0, 8, 3);
        var overlap = new RackWidgetService.Widget("C", "TREND", 1, source, null, null, null, null, 3, 4, 2, 2);
        var outside = new RackWidgetService.Widget("D", "VALUE", 1, source, null, null, null, null, 11, 0, 2, 2);
        assertDoesNotThrow(() -> RackWidgetService.validateWidgets(List.of(first,beside), List.of(available)));
        assertThrows(IllegalArgumentException.class, () -> RackWidgetService.validateWidgets(List.of(first,overlap), List.of(available)));
        assertThrows(IllegalArgumentException.class, () -> RackWidgetService.validateWidgets(List.of(outside), List.of(available)));
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        assertEquals(first, mapper.readValue(mapper.writeValueAsString(first), RackWidgetService.Widget.class));
    }
    @Test void rackAssignmentsRequireActualPathAndThreePhaseCapability() {
        var a = new RackWidgetService.Source(8,"WATT","snmp");
        var b = new RackWidgetService.Source(9,"WATT","snmp");
        var path = new RackWidgetService.Widget("경로", "PATH_BALANCE", 1, List.of(a,b));
        var aDevice = new RackWidgetService.RackWidgetDevice(8,"A PDU","model","PDU","A","A",false);
        var bDevice = new RackWidgetService.RackWidgetDevice(9,"B PDU","model","PDU","B","B",true);
        assertDoesNotThrow(() -> RackWidgetService.validateRackAssignments(List.of(path), java.util.Map.of(8,aDevice,9,bDevice)));
        assertThrows(IllegalArgumentException.class, () -> RackWidgetService.validateRackAssignments(List.of(path), java.util.Map.of(8,bDevice,9,aDevice)));
        var phase = new RackWidgetService.Widget("상", "PHASE_BALANCE", 1, List.of(a,a,a));
        assertThrows(IllegalArgumentException.class, () -> RackWidgetService.validateRackAssignments(List.of(phase), java.util.Map.of(8,aDevice)));
    }
    @Test void rejectsDuplicateNamesAndEquivalentDefinitionsWithinRack() {
        var other = new DeviceMeasurementSourceCatalog.Source(9, "PDU B", 1, "snmp", "WATT", "W", null, 9, "MODEL_POINT", false);
        var a = new RackWidgetService.Source(8,"WATT","snmp");
        var b = new RackWidgetService.Source(9,"WATT","snmp");
        var valid = List.of(available, other);
        var original = new RackWidgetService.Widget(" Power ", "VALUE", 1, List.of(a));
        var sameName = new RackWidgetService.Widget("power", "VALUE", 1, List.of(b));
        var sameSource = new RackWidgetService.Widget("another", "VALUE", 1, List.of(a));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> RackWidgetService.validateWidgets(List.of(original,sameName), valid)).getMessage().contains("이미 존재하는 위젯"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> RackWidgetService.validateWidgets(List.of(original,sameSource), valid)).getMessage().contains("이미 존재하는 위젯"));
        var pieA = new RackWidgetService.Widget("비율 1", "PIE", 1, List.of(a,b));
        var pieB = new RackWidgetService.Widget("비율 2", "PIE", 1, List.of(b,a));
        assertThrows(IllegalArgumentException.class, () -> RackWidgetService.validateWidgets(List.of(pieA,pieB), valid));
        var trendA = new RackWidgetService.Widget("하루", "POWER_TREND", 1, List.of(a,b), 24, 15);
        var trendB = new RackWidgetService.Widget("한시간", "POWER_TREND", 1, List.of(a,b), 1, 15);
        var trendDefault = new RackWidgetService.Widget("기본", "POWER_TREND", 1, List.of(a,b), null, null);
        assertDoesNotThrow(() -> RackWidgetService.validateWidgets(List.of(trendA,trendB), valid));
        assertThrows(IllegalArgumentException.class, () -> RackWidgetService.validateWidgets(List.of(trendA,trendDefault), valid));
        assertDoesNotThrow(() -> RackWidgetService.validateWidgets(List.of(original), valid));
    }
}
