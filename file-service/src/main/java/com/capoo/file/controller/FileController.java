package com.capoo.file.controller;

import com.capoo.file.dto.ApiResponse;
import com.capoo.file.dto.reponse.FileData;
import com.capoo.file.dto.reponse.FileReponse;
import com.capoo.file.service.FileService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
public class FileController {
    FileService fileService;
    @PostMapping("/media/upload")
    public ApiResponse<FileReponse> uploadFile(@RequestParam("file") MultipartFile file) {
        return ApiResponse.<FileReponse>builder()
                .result(fileService.uploadFile(file))
                .build();
    }
    @GetMapping("/media/download/{filename}")
    public ResponseEntity<Resource> downloadMediaFile(@PathVariable String filename) throws IOException {
         FileData fileData= fileService.downloadFile(filename);
         return ResponseEntity.<Resource>ok()
                 .header(HttpHeaders.CONTENT_TYPE,fileData.contentType())
                 .body(fileData.resource());
    }
}
