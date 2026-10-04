package com.common.storage.config;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import com.common.storage.storage.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;
import java.util.Locale;

@Configuration
@EnableConfigurationProperties(StorageObjectProperties.class)
public class ObjectStorageConfiguration {

    @Bean
    public ObjectStorageService objectStorageService(StorageObjectProperties storageObjectProperties) {
        String mode = storageObjectProperties.getMode() == null
                ? "local"
                : storageObjectProperties.getMode().toLowerCase(Locale.ROOT);

        return switch (mode) {
            case "s3" -> this.createS3ObjectStorageService(storageObjectProperties);
            default -> new LocalObjectStorageService(storageObjectProperties.getLocal().getRoot());
        };
    }

    private ObjectStorageService createS3ObjectStorageService(StorageObjectProperties storageObjectProperties) {
        StorageObjectProperties.S3 s3 = storageObjectProperties.getS3();
        if (!StringUtils.hasText(s3.getBucket())) {
            throw new IllegalArgumentException("Missing required property: storage.object.s3.bucket");
        }
        if (!StringUtils.hasText(s3.getRegion())) {
            throw new IllegalArgumentException("Missing required property: storage.object.s3.region");
        }

        software.amazon.awssdk.services.s3.S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(s3.getRegion()));

        if (StringUtils.hasText(s3.getEndpoint())) {
            builder = builder.endpointOverride(URI.create(s3.getEndpoint()));
        }

        S3Configuration s3Configuration = S3Configuration.builder()
                .pathStyleAccessEnabled(s3.isPathStyleAccessEnabled())
                .build();
        builder = builder.serviceConfiguration(s3Configuration);

        StorageObjectProperties.Credentials credentials = s3.getCredentials();
        if (credentials != null
                && "static".equalsIgnoreCase(credentials.getMode())
                && StringUtils.hasText(credentials.getAccessKey())
                && StringUtils.hasText(credentials.getSecretKey())) {
            AwsBasicCredentials awsBasicCredentials = AwsBasicCredentials.create(
                    credentials.getAccessKey(),
                    credentials.getSecretKey());
            builder = builder.credentialsProvider(StaticCredentialsProvider.create(awsBasicCredentials));
        } else {
            builder = builder.credentialsProvider(DefaultCredentialsProvider.create());
        }
        return new S3ObjectStorageService(builder.build(), s3.getBucket());
    }




}
