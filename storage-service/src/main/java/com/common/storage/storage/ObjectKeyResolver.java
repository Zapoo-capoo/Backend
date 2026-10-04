package com.common.storage.storage;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ObjectKeyResolver {

    private static final String WEBP_EXTENSION = ".webp";

    private final StorageObjectProperties storageObjectProperties;

    public ObjectKeyResolver(StorageObjectProperties storageObjectProperties) {
        this.storageObjectProperties = storageObjectProperties;
    }

    public String original(String fileId, String extension) {
        return this.withPrefix(fileId + extension);
    }

    public String web(String fileId) {
        return this.withPrefix("image/web/" + fileId + WEBP_EXTENSION);
    }

    public String mobile(String fileId) {
        return this.withPrefix("image/mobile/" + fileId + WEBP_EXTENSION);
    }

    public String thumbnail(String fileId) {
        return this.withPrefix("thumbnail/" + fileId + WEBP_EXTENSION);
    }

    public String video(String fileId, String quality, String extension) {
        return this.withPrefix("video/" + quality + "/" + fileId + extension);
    }

    public String hls(String fileId, String fileName) {
        return this.withPrefix("hls/" + fileId + "/" + fileName);
    }

    private String withPrefix(String rawKey) {
        String key = rawKey.startsWith("/") ? rawKey.substring(1) : rawKey;
        String basePrefix = this.storageObjectProperties.getBasePrefix();
        if (!StringUtils.hasText(basePrefix)) {
            return key;
        }
        String normalizedBasePrefix = basePrefix.startsWith("/") ? basePrefix.substring(1) : basePrefix;
        normalizedBasePrefix = normalizedBasePrefix.endsWith("/")
                ? normalizedBasePrefix.substring(0, normalizedBasePrefix.length() - 1)
                : normalizedBasePrefix;
        return normalizedBasePrefix + "/" + key;
    }
}
