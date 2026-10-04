package com.capoo.chat.repository.httpclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.fasterxml.jackson.annotation.JsonProperty;

@FeignClient(name = "storage-service", url = "${app.services.storage.url:http://localhost:7881/storage-service}")
public interface StorageClient {

    /**
     * Info about a file that a client uploaded to storage-service. Storage answers HTTP 200 even when the file does not
     * exist, the outcome is in {@link Response#success()}.
     */
    @GetMapping("/internal/files/{id}")
    Response<StoredFile> getFile(@PathVariable("id") String id);

    record Response<T>(Boolean success, T data, Object error) {}

    /** {@code type} is the MIME type (like image/png or video/mp4), {@code extension} includes the dot. */
    record StoredFile(
            String id, String extension, @JsonProperty("original_name") String originalName, String type, Long size) {}
}
