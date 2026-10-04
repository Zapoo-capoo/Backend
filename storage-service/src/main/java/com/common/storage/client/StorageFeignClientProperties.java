package com.common.storage.client;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "storage.client")
public class StorageFeignClientProperties {

    private String url;
    private String path = "/storage/internal/files";
}
