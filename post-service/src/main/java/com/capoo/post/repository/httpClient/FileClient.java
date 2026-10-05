package com.capoo.post.repository.httpClient;

import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.capoo.dto.ApiResponse;
import com.capoo.post.dto.response.FileReponse;

import lombok.RequiredArgsConstructor;

/** Uploads media to storage-service and returns the id it gave to the file. */
@Service
@RequiredArgsConstructor
public class FileClient {

    private final StorageClient storageClient;

    public ApiResponse<FileReponse> uploadMedia(MultipartFile file) {
        var response = storageClient.upload(file);
        if (response == null || !Boolean.TRUE.equals(response.success()) || response.data() == null) {
            throw new IllegalStateException("Storage service upload failed: " + (response == null ? null : response.error()));
        }
        var stored = response.data();
        return ApiResponse.<FileReponse>builder()
                .result(FileReponse.builder()
                        .id(stored.id())
                        .mediaType(mediaTypeOf(stored.type()))
                        .originalFileName(stored.originalName())
                        .build())
                .build();
    }

    // From the mime type storage detected, like image/png or video/mp4
    private String mediaTypeOf(String mimeType) {
        String mime = mimeType == null ? "" : mimeType.toLowerCase(Locale.ROOT);
        if (mime.startsWith("image/")) return "IMAGE";
        if (mime.startsWith("video/")) return "VIDEO";
        return "FILE";
    }
}
