package com.common.storage.client;

import com.common.storage.model.dto.FileDTO;
import com.common.storage.model.dto.ResponseModel;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@FeignClient(
        name = "storage-service",
        url = "${storage.client.url}",
        path = "${storage.client.path:/storage/internal/files}",
        configuration = StorageFeignClientConfig.class)
public interface StorageFileClient {

    @PostMapping(
            value = "/upload-file",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseModel<FileDTO> uploadFile(
            @RequestParam(value = "description", required = false) String description,
            @RequestPart("file") MultipartFile multipartFile);

    @PostMapping(
            value = "/upload-files",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseModel<List<FileDTO>> uploadFiles(
            @RequestParam(value = "description", required = false) String description,
            @RequestPart("files") MultipartFile[] multipartFiles);

    @GetMapping("/{id}")
    ResponseModel<FileDTO> getFile(@PathVariable("id") String id);

    @DeleteMapping("/{id}")
    ResponseModel<Void> deleteFile(@PathVariable("id") String id);
}
