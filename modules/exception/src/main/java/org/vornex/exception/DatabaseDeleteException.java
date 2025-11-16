package org.vornex.exception;

public class DatabaseDeleteException extends RuntimeException {
    public DatabaseDeleteException(String message) {
        super(message);
    }

    public DatabaseDeleteException(String message, Throwable cause) {
        super(message, cause);
    }
}
