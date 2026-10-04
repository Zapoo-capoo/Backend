package com.capoo.chat.repository.httpclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.annotation.JsonProperty;

@FeignClient(name = "storage-service", url = "${app.services.storage.url:http://localhost:7881/storage-service}")
public interface StorageClient {

    @PostMapping(value = "/internal/files/upload-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Response<StoredFile> upload(@RequestPart("file") MultipartFile file);

    record Response<T>(Boolean success, T data, Object error) {}

    record StoredFile(String id, String extension, @JsonProperty("original_name") String originalName) {}
}
