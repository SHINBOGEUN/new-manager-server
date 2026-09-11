package net.vivans.dcim.module.query.application;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class WidgetDataStatusResolverTest {

    private final WidgetDataStatusResolver resolver = new WidgetDataStatusResolver();

    @Test
    void resolvesNormalWhenEverySourceIsFresh() {
        var result = resolver.resolve(Arrays.asList(Instant.now().minusSeconds(30), Instant.now().minusSeconds(60)), 15);

        assertThat(result.status()).isEqualTo("NORMAL");
        assertThat(result.availableSourceCount()).isEqualTo(2);
    }

    @Test
    void resolvesPartialWhenOneSourceIsMissingOrStale() {
        var result = resolver.resolve(Arrays.asList(Instant.now().minusSeconds(30), null, Instant.now().minusSeconds(3600)), 15);

        assertThat(result.status()).isEqualTo("PARTIAL");
        assertThat(result.missingSourceCount()).isEqualTo(1);
        assertThat(result.staleSourceCount()).isEqualTo(1);
    }

    @Test
    void resolvesStaleOrMissingWhenNoFreshValueExists() {
        assertThat(resolver.resolve(Arrays.asList(Instant.now().minusSeconds(3600)), 15).status()).isEqualTo("STALE");
        assertThat(resolver.resolve(Arrays.asList(null, null), 15).status()).isEqualTo("MISSING");
    }
}
