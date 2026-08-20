package net.vivans.dcim.module.collectortask.infrastructure.collector;

import org.springframework.web.client.RestClientResponseException;

import java.util.function.Supplier;

final class CollectorSyncRetryExecutor {

    private CollectorSyncRetryExecutor() {
    }

    static <T> T execute(Supplier<T> action, int maxAttempts, long delayMs) {
        int attempts = Math.max(maxAttempts, 1);
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                return action.get();
            } catch (RuntimeException exception) {
                lastFailure = exception;
                if (attempt >= attempts || !isRetryable(exception)) {
                    throw exception;
                }
                sleep(delayMs);
            }
        }
        throw lastFailure;
    }

    static void executeVoid(Runnable action, int maxAttempts, long delayMs) {
        execute(() -> {
            action.run();
            return null;
        }, maxAttempts, delayMs);
    }

    private static boolean isRetryable(RuntimeException exception) {
        if (exception instanceof RestClientResponseException restException) {
            int status = restException.getStatusCode().value();
            return status >= 500 || status == 429;
        }
        return !(exception instanceof IllegalStateException);
    }

    private static void sleep(long delayMs) {
        if (delayMs <= 0) {
            return;
        }
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("collector sync retry interrupted", ex);
        }
    }
}
