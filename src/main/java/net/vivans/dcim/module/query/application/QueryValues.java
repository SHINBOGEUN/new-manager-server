package net.vivans.dcim.module.query.application;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class QueryValues {

    private QueryValues() {
    }

    static BigDecimal round2(Double value) {
        if (value == null || value.isNaN() || value.isInfinite()) {
            return null;
        }
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    static BigDecimal round2(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
