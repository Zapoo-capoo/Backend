package com.capoo.chat.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.capoo.chat.entity.Attachment;
import com.capoo.chat.exception.AppException;
import com.capoo.chat.exception.ErrorCode;
import com.capoo.chat.repository.httpclient.StorageClient;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;

/**
 * Turns the file ids a client sends with a message into attachments. The client uploads the files to storage-service
 * itself, so every id is checked here: it must exist, be an image or a video, and there are at most
 * {@value #MAX_ATTACHMENTS}. Type, name, size and links are taken from storage, never from the client.
 */
@Slf4j
@Service
public class AttachmentResolver {
    public static final int MAX_ATTACHMENTS = 5;

    private final StorageClient storageClient;
    private final String publicUrl;

    public AttachmentResolver(
            StorageClient storageClient,
            @Value("${app.storage.public-url:http://localhost:8888/api/v1/storage-service/files}") String publicUrl) {
        this.storageClient = storageClient;
        this.publicUrl = publicUrl;
    }

    /** Attachments in the order the ids were given. Blank and repeated ids are ignored. */
    public List<Attachment> resolve(List<String> fileIds) {
        Set<String> ids = new LinkedHashSet<>();
        if (fileIds != null) {
            fileIds.stream()
                    .filter(id -> id != null && !id.isBlank())
                    .map(String::trim)
                    .forEach(ids::add);
        }
        if (ids.size() > MAX_ATTACHMENTS) {
            throw new AppException(ErrorCode.TOO_MANY_ATTACHMENTS);
        }

        // Everything is checked before the message is saved, one bad id rejects the whole message
        List<Attachment> attachments = new ArrayList<>();
        ids.forEach(id -> attachments.add(toAttachment(id)));
        return attachments;
    }

    private Attachment toAttachment(String fileId) {
        if (!isPlainFileId(fileId)) {
            // The id ends up in a URL path towards storage-service, so anything that could change the path is refused
            throw new AppException(ErrorCode.ATTACHMENT_NOT_FOUND);
        }

        StorageClient.Response<StorageClient.StoredFile> response;
        try {
            response = storageClient.getFile(fileId);
        } catch (FeignException e) {
            // storage-service answers 404/400, and in practice 500, for an id it does not know. A storage that is
            // unreachable (no HTTP status at all) is not a bad id, so that one is left to propagate
            if (e.status() == 404 || e.status() == 400 || e.status() == 500) {
                log.warn("Storage answered {} for attachment {}", e.status(), fileId);
                throw new AppException(ErrorCode.ATTACHMENT_NOT_FOUND);
            }
            throw e;
        }
        if (response == null || !Boolean.TRUE.equals(response.success()) || response.data() == null) {
            throw new AppException(ErrorCode.ATTACHMENT_NOT_FOUND);
        }

        StorageClient.StoredFile file = response.data();
        String mimeType = file.type() == null ? "" : file.type().toLowerCase(Locale.ROOT);
        String extension = file.extension() == null ? "" : file.extension();
        if (!extension.isEmpty() && !extension.startsWith(".")) {
            extension = "." + extension;
        }

        if (mimeType.startsWith("image/")) {
            return Attachment.builder()
                    .fileId(file.id())
                    .type(Attachment.IMAGE)
                    .url(publicUrl + "/web/" + file.id() + ".webp")
                    .thumbnailUrl(publicUrl + "/thumbnail/" + file.id() + ".webp")
                    .name(file.originalName())
                    .size(file.size())
                    .build();
        }
        if (mimeType.startsWith("video/")) {
            // stream-video supports range requests (seeking), download would force the browser to save the file.
            // Storage cuts a poster frame from the video in the background, served at the same thumbnail endpoint
            return Attachment.builder()
                    .fileId(file.id())
                    .type(Attachment.VIDEO)
                    .url(publicUrl + "/stream-video/" + file.id() + extension)
                    .thumbnailUrl(publicUrl + "/thumbnail/" + file.id() + ".webp")
                    .name(file.originalName())
                    .size(file.size())
                    .build();
        }
        throw new AppException(ErrorCode.UNSUPPORTED_ATTACHMENT_TYPE);
    }

    private boolean isPlainFileId(String fileId) {
        return fileId.length() <= 200
                && !fileId.contains("/")
                && !fileId.contains("\\")
                && !fileId.contains("..")
                && !fileId.contains("?")
                && !fileId.contains("#")
                && !fileId.contains("%");
    }
}
