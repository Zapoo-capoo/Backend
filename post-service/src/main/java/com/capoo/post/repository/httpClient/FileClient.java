package com.capoo.post.repository.httpClient;

import java.util.Locale;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.capoo.post.dto.response.FileReponse;
import com.capoo.dto.ApiResponse;

/** Uploads media to storage-service and exposes the result in the shape callers already use. */
@Service
public class FileClient {

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg", "gif", "webp");

    private final StorageClient storageClient;
    private final String publicUrl;

    public FileClient(
            StorageClient storageClient,
            @Value("${app.storage.public-url:http://localhost:8888/api/v1/storage-service/files}") String publicUrl) {
        this.storageClient = storageClient;
        this.publicUrl = publicUrl;
    }

    public ApiResponse<FileReponse> uploadMedia(MultipartFile file) {
        var response = storageClient.upload(file);
        if (response == null || !Boolean.TRUE.equals(response.success()) || response.data() == null) {
            throw new IllegalStateException("Storage service upload failed: " + (response == null ? null : response.error()));
        }
        var stored = response.data();
        String ext = stored.extension() == null ? "" : stored.extension().replace(".", "").toLowerCase(Locale.ROOT);
        String url = IMAGE_EXTENSIONS.contains(ext)
                ? publicUrl + "/web/" + stored.id() + ".webp"
                : publicUrl + "/download/" + stored.id();
        return ApiResponse.<FileReponse>builder()
                .result(FileReponse.builder().originalFileName(stored.originalName()).url(url).build())
                .build();
    }
}
