package org.vornex.exception;

public class StorageDeleteException extends RuntimeException {
    public StorageDeleteException(String message) {
        super(message);
    }

    public StorageDeleteException(String message, Throwable cause) {
        super(message, cause);
    }
}
