package com.common.storage.storage;

import com.common.storage.exception.ObjectStorageException;
import com.common.storage.exception.ObjectStorageNotFoundException;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class LocalObjectStorageService implements ObjectStorageService {

    private final Path root;

    public LocalObjectStorageService(String rootPath) {
        this.root = Path.of(rootPath).normalize().toAbsolutePath();
    }

    @Override
    public void putObject(String key, InputStream inputStream, String contentType, Long contentLength) throws IOException {
        Path targetPath = this.resolvePath(key);
        Path parent = targetPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public InputStream getObject(String key) throws IOException {
        Path sourcePath = this.resolvePath(key);
        if (!Files.exists(sourcePath)) {
            throw new ObjectStorageNotFoundException("Object not found: " + key);
        }
        return Files.newInputStream(sourcePath);
    }

    @Override
    public byte[] getObjectRange(String key, long start, long end) throws IOException {
        Path sourcePath = this.resolvePath(key);
        if (!Files.exists(sourcePath)) {
            throw new ObjectStorageNotFoundException("Object not found: " + key);
        }
        long size = Files.size(sourcePath);
        if (size == 0 || start >= size || end < start) {
            return new byte[0];
        }
        long boundedEnd = Math.min(end, size - 1);
        long length = boundedEnd - start + 1;
        if (length > Integer.MAX_VALUE) {
            throw new IOException("Requested range is too large");
        }

        byte[] data = new byte[(int) length];
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(sourcePath.toFile(), "r")) {
            randomAccessFile.seek(start);
            randomAccessFile.readFully(data);
        }
        return data;
    }

    @Override
    public ObjectMetadata head(String key) throws IOException {
        Path sourcePath = this.resolvePath(key);
        if (!Files.exists(sourcePath)) {
            throw new ObjectStorageNotFoundException("Object not found: " + key);
        }
        String contentType = Files.probeContentType(sourcePath);
        if (!StringUtils.hasText(contentType)) {
            contentType = "application/octet-stream";
        }
        return new ObjectMetadata(Files.size(sourcePath), contentType);
    }

    @Override
    public boolean exists(String key) {
        try {
            return Files.exists(this.resolvePath(key));
        } catch (ObjectStorageException ex) {
            return false;
        }
    }

    @Override
    public void delete(String key) throws IOException {
        Path sourcePath = this.resolvePath(key);
        Files.deleteIfExists(sourcePath);
    }

    @Override
    public void copy(String sourceKey, String targetKey) throws IOException {
        Path sourcePath = this.resolvePath(sourceKey);
        if (!Files.exists(sourcePath)) {
            throw new ObjectStorageNotFoundException("Object not found: " + sourceKey);
        }
        Path targetPath = this.resolvePath(targetKey);
        Path parent = targetPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
    }

    private Path resolvePath(String key) throws ObjectStorageException {
        Path resolved = this.root.resolve(key).normalize();
        if (!resolved.startsWith(this.root)) {
            throw new ObjectStorageException("Invalid object key: " + key);
        }
        return resolved;
    }
}
