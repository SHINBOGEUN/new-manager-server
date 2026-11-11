package net.vivans.dcim.module.query.application;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Formats unit-aware values for display while leaving the canonical value and unit intact. */
public final class UnitDisplay {
    private static final BigDecimal THOUSAND = BigDecimal.valueOf(1000);

    private UnitDisplay() {
    }

    public static Result of(BigDecimal value, String unit) {
        if (value == null) return new Result(null, unit);
        BigDecimal display = value;
        String displayUnit = unit;
        if (unit != null && value.compareTo(THOUSAND) >= 0) {
            if (unit.equalsIgnoreCase("W")) {
                display = value.divide(THOUSAND, 2, RoundingMode.HALF_UP);
                displayUnit = "kW";
            } else if (unit.equalsIgnoreCase("Wh")) {
                display = value.divide(THOUSAND, 2, RoundingMode.HALF_UP);
                displayUnit = "kWh";
            }
        }
        return new Result(display.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString(), displayUnit);
    }

    public record Result(String displayValue, String displayUnit) {
    }
}
