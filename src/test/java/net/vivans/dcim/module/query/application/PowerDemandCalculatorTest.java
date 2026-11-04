package net.vivans.dcim.module.query.application;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import java.util.List;
import net.vivans.dcim.module.query.domain.SeriesPoint;
import org.junit.jupiter.api.Test;

class PowerDemandCalculatorTest {
    @Test void findsPeakOfCompleteWindowsAndIgnoresIncompleteOrPartialWindows() {
        Instant start = Instant.parse("2026-11-04T00:00:00Z");
        Instant end = Instant.parse("2026-11-04T00:37:00Z");
        var points = List.of(
                new SeriesPoint(1, "POWER", 10.0, start.plusSeconds(900)),
                new SeriesPoint(2, "POWER", 20.0, start.plusSeconds(900)),
                new SeriesPoint(1, "POWER", 99.0, start.plusSeconds(1800)),
                new SeriesPoint(1, "POWER", 1000.0, end),
                new SeriesPoint(2, "POWER", 1000.0, end));
        var peak = PowerDemandCalculator.peak(points, List.of(1, 2), start, end);
        assertNotNull(peak);
        assertEquals(start, peak.time());
        assertEquals(30.0, peak.value());
        assertNull(PowerDemandCalculator.peak(points, List.of(1, 2), start.plusSeconds(1), end));
    }
    @Test void tiesUseEarliestCompleteWindow() {
        Instant start = Instant.parse("2026-11-04T00:00:00Z");
        var points = List.of(new SeriesPoint(1, "POWER", 5.0, start.plusSeconds(1800)),
                new SeriesPoint(1, "POWER", 5.0, start.plusSeconds(900)));
        assertEquals(start, PowerDemandCalculator.peak(points, List.of(1), start, start.plusSeconds(1800)).time());
    }
}
