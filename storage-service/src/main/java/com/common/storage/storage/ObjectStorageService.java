package com.common.storage.storage;

import java.io.IOException;
import java.io.InputStream;

public interface ObjectStorageService {

    void putObject(String key, InputStream inputStream, String contentType, Long contentLength) throws IOException;

    InputStream getObject(String key) throws IOException;

    byte[] getObjectRange(String key, long start, long end) throws IOException;

    ObjectMetadata head(String key) throws IOException;

    boolean exists(String key);

    void delete(String key) throws IOException;

    void copy(String sourceKey, String targetKey) throws IOException;
}
