package com.common.storage.exception;

import java.io.IOException;

public class ObjectStorageException extends IOException {

    public ObjectStorageException(String message) {
        super(message);
    }

    public ObjectStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
