package com.common.storage.exception;

public class MaxUploadSizeExceedException extends Exception {

    public MaxUploadSizeExceedException(String message) {
        super(message);
    }

    public MaxUploadSizeExceedException(String message, Throwable cause) {
        super(message, cause);
    }

    public MaxUploadSizeExceedException(Throwable cause) {
        super(cause);
    }
}
