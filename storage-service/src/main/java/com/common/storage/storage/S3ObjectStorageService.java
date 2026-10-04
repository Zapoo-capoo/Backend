package com.common.storage.storage;

import com.common.storage.exception.ObjectStorageException;
import com.common.storage.exception.ObjectStorageNotFoundException;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.InputStream;

public class S3ObjectStorageService implements ObjectStorageService {

    private final S3Client s3Client;
    private final String bucket;

    public S3ObjectStorageService(S3Client s3Client, String bucket) {
        this.s3Client = s3Client;
        this.bucket = bucket;
    }

    @Override
    public void putObject(String key, InputStream inputStream, String contentType, Long contentLength) throws IOException {
        PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
                .bucket(this.bucket)
                .key(key);
        if (contentType != null && !contentType.isBlank()) {
            requestBuilder.contentType(contentType);
        }

        try {
            RequestBody requestBody;
            if (contentLength != null && contentLength >= 0) {
                requestBody = RequestBody.fromInputStream(inputStream, contentLength);
            } else {
                requestBody = RequestBody.fromBytes(inputStream.readAllBytes());
            }
            this.s3Client.putObject(requestBuilder.build(), requestBody);
        } catch (S3Exception ex) {
            throw new ObjectStorageException("Failed to upload object to S3: " + key, ex);
        }
    }

    @Override
    public InputStream getObject(String key) throws IOException {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(this.bucket)
                .key(key)
                .build();
        try {
            ResponseInputStream<GetObjectResponse> responseInputStream = this.s3Client.getObject(request);
            return responseInputStream;
        } catch (NoSuchKeyException ex) {
            throw new ObjectStorageNotFoundException("Object not found in S3: " + key, ex);
        } catch (S3Exception ex) {
            if (ex.statusCode() == 404) {
                throw new ObjectStorageNotFoundException("Object not found in S3: " + key, ex);
            }
            throw new ObjectStorageException("Failed to download object from S3: " + key, ex);
        }
    }

    @Override
    public byte[] getObjectRange(String key, long start, long end) throws IOException {
        String range = "bytes=" + start + "-" + end;
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(this.bucket)
                .key(key)
                .range(range)
                .build();
        try {
            ResponseBytes<GetObjectResponse> objectBytes = this.s3Client.getObjectAsBytes(request);
            return objectBytes.asByteArray();
        } catch (NoSuchKeyException ex) {
            throw new ObjectStorageNotFoundException("Object not found in S3: " + key, ex);
        } catch (S3Exception ex) {
            if (ex.statusCode() == 404) {
                throw new ObjectStorageNotFoundException("Object not found in S3: " + key, ex);
            }
            throw new ObjectStorageException("Failed to read range from S3 object: " + key, ex);
        }
    }

    @Override
    public ObjectMetadata head(String key) throws IOException {
        HeadObjectRequest request = HeadObjectRequest.builder()
                .bucket(this.bucket)
                .key(key)
                .build();
        try {
            HeadObjectResponse response = this.s3Client.headObject(request);
            return new ObjectMetadata(response.contentLength(), response.contentType());
        } catch (NoSuchKeyException ex) {
            throw new ObjectStorageNotFoundException("Object not found in S3: " + key, ex);
        } catch (S3Exception ex) {
            if (ex.statusCode() == 404) {
                throw new ObjectStorageNotFoundException("Object not found in S3: " + key, ex);
            }
            throw new ObjectStorageException("Failed to read metadata from S3 object: " + key, ex);
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            this.head(key);
            return true;
        } catch (ObjectStorageNotFoundException ex) {
            return false;
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public void delete(String key) throws IOException {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(this.bucket)
                .key(key)
                .build();
        try {
            this.s3Client.deleteObject(request);
        } catch (S3Exception ex) {
            throw new ObjectStorageException("Failed to delete S3 object: " + key, ex);
        }
    }

    @Override
    public void copy(String sourceKey, String targetKey) throws IOException {
        CopyObjectRequest request = CopyObjectRequest.builder()
                .sourceBucket(this.bucket)
                .sourceKey(sourceKey)
                .destinationBucket(this.bucket)
                .destinationKey(targetKey)
                .build();
        try {
            this.s3Client.copyObject(request);
        } catch (S3Exception ex) {
            if (ex.statusCode() == 404) {
                throw new ObjectStorageNotFoundException("Source object not found in S3: " + sourceKey, ex);
            }
            throw new ObjectStorageException("Failed to copy S3 object: " + sourceKey, ex);
        }
    }
}
