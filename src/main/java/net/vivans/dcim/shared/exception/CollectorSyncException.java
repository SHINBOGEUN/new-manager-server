package net.vivans.dcim.shared.exception;

public class CollectorSyncException extends RuntimeException {

    public CollectorSyncException(String message) {
        super(message);
    }

    public CollectorSyncException(String message, Throwable cause) {
        super(message, cause);
    }
}
