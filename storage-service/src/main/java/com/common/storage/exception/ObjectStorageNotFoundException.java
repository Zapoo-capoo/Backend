package com.common.storage.exception;

import java.io.IOException;

public class ObjectStorageNotFoundException extends IOException {

    public ObjectStorageNotFoundException(String message) {
        super(message);
    }

    public ObjectStorageNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
