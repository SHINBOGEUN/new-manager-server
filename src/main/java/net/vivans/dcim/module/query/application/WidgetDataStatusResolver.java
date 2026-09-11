package net.vivans.dcim.module.query.application;

import net.vivans.dcim.module.query.api.dto.WidgetDataStatusResponse;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collection;

/** 모든 데이터 위젯이 동일한 기준으로 수집 신선도를 판단한다. */
@Component
public class WidgetDataStatusResolver {

    public static final int DEFAULT_FRESHNESS_MINUTES = 15;

    public WidgetDataStatusResponse resolve(Collection<Instant> collectedTimes, Integer configuredFreshnessMinutes) {
        int freshnessMinutes = normalizeFreshnessMinutes(configuredFreshnessMinutes);
        Instant staleBefore = Instant.now().minusSeconds(freshnessMinutes * 60L);
        int expected = collectedTimes == null ? 0 : collectedTimes.size();
        int available = 0;
        int missing = 0;
        int stale = 0;
        Instant latest = null;

        if (collectedTimes != null) {
            for (Instant collectedAt : collectedTimes) {
                if (collectedAt == null) {
                    missing++;
                    continue;
                }
                available++;
                if (latest == null || collectedAt.isAfter(latest)) {
                    latest = collectedAt;
                }
                if (collectedAt.isBefore(staleBefore)) {
                    stale++;
                }
            }
        }

        String status;
        if (expected == 0 || available == 0) {
            status = "MISSING";
        } else if (missing > 0 || stale > 0) {
            status = (stale == available && missing == 0) ? "STALE" : "PARTIAL";
        } else {
            status = "NORMAL";
        }
        return new WidgetDataStatusResponse(status, latest, freshnessMinutes,
                expected, available, missing, stale);
    }

    public static int normalizeFreshnessMinutes(Integer value) {
        if (value == null || value <= 0) {
            return DEFAULT_FRESHNESS_MINUTES;
        }
        if (value > 1440) {
            throw new IllegalArgumentException("dataFreshnessMinutes must be between 1 and 1440");
        }
        return value;
    }
}
