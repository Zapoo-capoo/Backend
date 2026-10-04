package com.common.storage.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "storage.object")
public class StorageObjectProperties {

    private String mode = "local";
    private String basePrefix = "";
    private boolean readFallbackLocalEnabled = true;
    private Local local = new Local();
    private S3 s3 = new S3();
    private Azure azure = new Azure();

    @Data
    public static class Local {
        private String root = "./home/media";
    }

    @Data
    public static class S3 {
        private String bucket;
        private String region;
        private String endpoint;
        private boolean pathStyleAccessEnabled;
        private Credentials credentials = new Credentials();
    }

    @Data
    public static class Azure {
        private String container;
        private String accountName;
        private String accountKey;
        private String endpoint;
        private Credentials credentials = new Credentials();
    }

    @Data
    public static class Credentials {
        private String mode = "default";
        private String accessKey;
        private String secretKey;
        private String connectionString;
    }
}
