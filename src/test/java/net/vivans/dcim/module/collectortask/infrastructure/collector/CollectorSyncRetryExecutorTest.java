package net.vivans.dcim.module.collectortask.infrastructure.collector;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientResponseException;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CollectorSyncRetryExecutorTest {

    @Test
    void retriesRetryableFailures() {
        AtomicInteger attempts = new AtomicInteger();

        Integer result = CollectorSyncRetryExecutor.execute(() -> {
            if (attempts.incrementAndGet() < 3) {
                throw new RestClientResponseException("server error", 503, "503", null, null, null);
            }
            return 42;
        }, 3, 0);

        assertThat(result).isEqualTo(42);
        assertThat(attempts.get()).isEqualTo(3);
    }

    @Test
    void doesNotRetryNonRetryableFailures() {
        AtomicInteger attempts = new AtomicInteger();

        assertThatThrownBy(() -> CollectorSyncRetryExecutor.execute((Supplier<Integer>) () -> {
            attempts.incrementAndGet();
            throw new RestClientResponseException("bad request", 400, "400", null, null, null);
        }, 3, 0)).isInstanceOf(RestClientResponseException.class);

        assertThat(attempts.get()).isEqualTo(1);
    }
}
