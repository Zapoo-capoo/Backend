package com.common.storage.service.impl;

import com.common.storage.constant.SUPPORT_FILE_TYPE;
import com.common.storage.exception.NotFoundException;
import com.common.storage.exception.ObjectStorageNotFoundException;
import com.common.storage.model.dto.StreamingMediaDTO;
import com.common.storage.model.entity.jpa.File;
import com.common.storage.repository.FileRepository;
import com.common.storage.service.IFileMediaDeliveryService;
import com.common.storage.service.support.FilePathResolver;
import com.common.storage.storage.ObjectKeyResolver;
import com.common.storage.storage.ObjectMetadata;
import com.common.storage.storage.ObjectStorageService;
import com.common.storage.storage.StorageObjectProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.tika.Tika;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileMediaDeliveryService implements IFileMediaDeliveryService {

    private static final String FILE_NOT_FOUND = "File not found.";
    private static final String WEBP_TYPE = "image/webp";
    private static final String WEBP_EXTENSION = ".webp";

    private final FileRepository fileRepository;
    private final Tika tika;
    private final FilePathResolver filePathResolver;
    private final ObjectStorageService objectStorageService;
    private final ObjectKeyResolver objectKeyResolver;
    private final StorageObjectProperties storageObjectProperties;

    @Override
    public StreamingMediaDTO getWebFile(String fileName, String rangeHeader) throws IOException {
        String resolvedFileName = this.ensureWebp(fileName);
        String fileId = FileNameUtils.getBaseName(resolvedFileName);
        String objectKey = this.objectKeyResolver.web(fileId);
        Path legacyPath = Paths.get(this.filePathResolver.getWebUploadPath(), resolvedFileName);

        try {
            return this.buildStreamingMediaFromObject(objectKey, resolvedFileName, WEBP_TYPE, rangeHeader);
        } catch (ObjectStorageNotFoundException ex) {
            if (this.storageObjectProperties.isReadFallbackLocalEnabled() && Files.exists(legacyPath)) {
                return this.buildStreamingMediaFromLocal(legacyPath, WEBP_TYPE, rangeHeader);
            }
            File fallback = this.fileRepository.findById(fileId)
                    .orElseThrow(() -> new NotFoundException(FILE_NOT_FOUND));
            return this.downloadFile(fallback.getId() + fallback.getExtension(), rangeHeader);
        }
    }

    @Override
    public StreamingMediaDTO getThumbnailImage(String fileName) throws IOException {
        String resolvedFileName = this.ensureWebp(fileName);
        String fileId = FileNameUtils.getBaseName(resolvedFileName);
        String objectKey = this.objectKeyResolver.thumbnail(fileId);
        Path legacyPath = Paths.get(this.filePathResolver.getThumbnailUploadPath(), resolvedFileName);

        try {
            return this.buildStreamingMediaFromObject(objectKey, resolvedFileName, WEBP_TYPE, null);
        } catch (ObjectStorageNotFoundException ex) {
            if (this.storageObjectProperties.isReadFallbackLocalEnabled() && Files.exists(legacyPath)) {
                return this.buildStreamingMediaFromLocal(legacyPath, WEBP_TYPE, null);
            }
            File fallback = this.fileRepository.findById(fileId)
                    .orElseThrow(() -> new NotFoundException(FILE_NOT_FOUND));
            return this.downloadFile(fallback.getId() + fallback.getExtension(), null);
        }
    }
    @Override
    public StreamingMediaDTO downloadFile(String fileId, String rangeHeader) throws IOException {
        File file = this.fileRepository.findById(fileId)
                .orElseThrow(() -> new NotFoundException(FILE_NOT_FOUND));
        String extension = file.getExtension();
        String resolvedFileName = file.getId() + extension;
        String objectKey = this.objectKeyResolver.original(fileId, extension);
        Path legacyPath = Paths.get(this.filePathResolver.getUploadPath(), resolvedFileName);
        StreamingMediaDTO result;
        try {
            result = this.buildStreamingMediaFromObject(objectKey, resolvedFileName, null, rangeHeader);
        } catch (ObjectStorageNotFoundException ex) {
            if (this.storageObjectProperties.isReadFallbackLocalEnabled() && Files.exists(legacyPath)) {
                result = this.buildStreamingMediaFromLocal(legacyPath, null, rangeHeader);
            } else {
                throw new NotFoundException(FILE_NOT_FOUND);
            }
        }
        String downloadName = StringUtils.hasText(file.getOriginalName()) ? file.getOriginalName() : resolvedFileName;
        result.getHeaders().set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadName + "\"");
        return result;
    }
    @Override
    public ResponseEntity<StreamingResponseBody> streamVideoV2(String fileName, String quality, String rangeHeader)
            throws IOException {
        MediaLocation mediaLocation = this.resolveVideoLocation(fileName, quality);
        String mediaType = this.resolveContentType(mediaLocation.displayName(), null);
        long fileSize = mediaLocation.size();

        if (fileSize <= 0) {
            return ResponseEntity.status(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, mediaType)
                    .header(HttpHeaders.CONTENT_LENGTH, "0")
                    .body(outputStream -> {
                    });
        }

        if (!StringUtils.hasText(rangeHeader)) {
            StreamingResponseBody responseBody = outputStream -> {
                try (InputStream inputStream = mediaLocation.openStream()) {
                    IOUtils.copy(inputStream, outputStream, 1024);
                }
            };
            return ResponseEntity.status(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, mediaType)
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileSize))
                    .body(responseBody);
        }
        RangeRequest rangeRequest = this.parseRange(rangeHeader, fileSize);
        byte[] data = mediaLocation.readRange(rangeRequest.start(), rangeRequest.end());
        String contentLength = String.valueOf((rangeRequest.end() - rangeRequest.start()) + 1);
        StreamingResponseBody responseBody = outputStream -> outputStream.write(data);
        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .header(HttpHeaders.CONTENT_TYPE, mediaType)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CONTENT_LENGTH, contentLength)
                .header(HttpHeaders.CONTENT_RANGE,
                        "bytes " + rangeRequest.start() + "-" + rangeRequest.end() + "/" + fileSize)
                .body(responseBody);
    }

    private StreamingMediaDTO buildStreamingMediaFromObject(
            String objectKey, String fileName, String forceContentType, String rangeHeader) throws IOException {
        ObjectMetadata objectMetadata = this.objectStorageService.head(objectKey);
        String mediaType = StringUtils.hasText(forceContentType)
                ? forceContentType
                : this.resolveContentType(fileName, objectMetadata.contentType());

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set(HttpHeaders.CONTENT_TYPE, mediaType);

        if (StringUtils.hasText(rangeHeader) && mediaType.startsWith("video")) {
            RangeRequest rangeRequest = this.parseRange(rangeHeader, objectMetadata.size());
            byte[] rangeData = this.objectStorageService.getObjectRange(
                    objectKey, rangeRequest.start(), rangeRequest.end());
            responseHeaders.set(HttpHeaders.CONTENT_LENGTH, String.valueOf(rangeData.length));
            responseHeaders.set(HttpHeaders.CONTENT_RANGE,
                    "bytes " + rangeRequest.start() + "-" + rangeRequest.end() + "/" + objectMetadata.size());
            responseHeaders.set(HttpHeaders.ACCEPT_RANGES, "bytes");
            return StreamingMediaDTO.builder()
                    .headers(responseHeaders)
                    .media_type(mediaType)
                    .response_body(outputStream -> outputStream.write(rangeData))
                    .build();
        }

        responseHeaders.set(HttpHeaders.CONTENT_LENGTH, String.valueOf(objectMetadata.size()));
        InputStream inputStream = this.objectStorageService.getObject(objectKey);
        StreamingResponseBody stream = outputStream -> {
            try (InputStream sourceStream = inputStream) {
                IOUtils.copy(sourceStream, outputStream, 1024);
            }
        };
        return StreamingMediaDTO.builder()
                .headers(responseHeaders)
                .media_type(mediaType)
                .response_body(stream)
                .build();
    }

    private StreamingMediaDTO buildStreamingMediaFromLocal(Path path, String forceContentType, String rangeHeader)
            throws IOException {
        if (!Files.exists(path)) {
            throw new ObjectStorageNotFoundException("Legacy local file not found: " + path);
        }

        String mediaType = StringUtils.hasText(forceContentType)
                ? forceContentType
                : this.resolveContentType(path.getFileName().toString(), Files.probeContentType(path));
        long fileSize = Files.size(path);

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set(HttpHeaders.CONTENT_TYPE, mediaType);

        if (StringUtils.hasText(rangeHeader) && mediaType.startsWith("video")) {
            RangeRequest rangeRequest = this.parseRange(rangeHeader, fileSize);
            byte[] rangeData = new LocalMediaLocation(path, path.getFileName().toString())
                    .readRange(rangeRequest.start(), rangeRequest.end());
            responseHeaders.set(HttpHeaders.CONTENT_LENGTH, String.valueOf(rangeData.length));
            responseHeaders.set(HttpHeaders.CONTENT_RANGE,
                    "bytes " + rangeRequest.start() + "-" + rangeRequest.end() + "/" + fileSize);
            responseHeaders.set(HttpHeaders.ACCEPT_RANGES, "bytes");
            return StreamingMediaDTO.builder()
                    .headers(responseHeaders)
                    .media_type(mediaType)
                    .response_body(outputStream -> outputStream.write(rangeData))
                    .build();
        }

        responseHeaders.set(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileSize));
        StreamingResponseBody stream = outputStream -> {
            try (InputStream inputStream = Files.newInputStream(path)) {
                IOUtils.copy(inputStream, outputStream, 1024);
            }
        };
        return StreamingMediaDTO.builder()
                .headers(responseHeaders)
                .media_type(mediaType)
                .response_body(stream)
                .build();
    }

    private MediaLocation resolveVideoLocation(String fileName, String quality) throws IOException {
        String extension = FileNameUtils.getExtension(fileName);
        String fileId = FileNameUtils.getBaseName(fileName);

        if (StringUtils.hasText(extension)) {
            String mimeType = this.tika.detect(fileName);
            if (mimeType != null && (SUPPORT_FILE_TYPE.Audio.MIME_TYPES.contains(mimeType)
                    || SUPPORT_FILE_TYPE.Video.MIME_TYPES.contains(mimeType))) {
                String originalKey = this.objectKeyResolver.original(fileId, "." + extension);
                Path legacyPath = Paths.get(this.filePathResolver.getUploadPath(), fileId + "." + extension);
                MediaLocation originalLocation = this.resolveLocation(originalKey, legacyPath, fileName);
                if (originalLocation != null) {
                    return originalLocation;
                }
            }
        }

        String mp4VariantKey = this.objectKeyResolver.video(fileId, quality, ".mp4");
        Path legacyMp4Path = Paths.get(this.filePathResolver.getVideoUploadPath(quality), fileId + ".mp4");
        MediaLocation mp4Location = this.resolveLocation(mp4VariantKey, legacyMp4Path, fileId + ".mp4");
        if (mp4Location != null) {
            return mp4Location;
        }

        String webmVariantKey = this.objectKeyResolver.video(fileId, quality, ".webm");
        Path legacyWebmPath = Paths.get(this.filePathResolver.getVideoUploadPath(quality), fileId + ".webm");
        MediaLocation webmLocation = this.resolveLocation(webmVariantKey, legacyWebmPath, fileId + ".webm");
        if (webmLocation != null) {
            return webmLocation;
        }

        File file = this.fileRepository.findById(fileId)
                .orElseThrow(() -> new NotFoundException(FILE_NOT_FOUND));
        String originalKey = this.objectKeyResolver.original(file.getId(), file.getExtension());
        Path legacyOriginalPath = Paths.get(this.filePathResolver.getUploadPath(),
                file.getId() + file.getExtension());
        MediaLocation fallbackLocation = this.resolveLocation(
                originalKey, legacyOriginalPath, file.getId() + file.getExtension());
        if (fallbackLocation == null) {
            throw new NotFoundException(FILE_NOT_FOUND);
        }
        return fallbackLocation;
    }

    private MediaLocation resolveLocation(String objectKey, Path legacyPath, String displayName) throws IOException {
        if (this.objectStorageService.exists(objectKey)) {
            ObjectMetadata metadata = this.objectStorageService.head(objectKey);
            return new ObjectMediaLocation(this.objectStorageService, objectKey, metadata.size(), displayName);
        }
        if (this.storageObjectProperties.isReadFallbackLocalEnabled() && Files.exists(legacyPath)) {
            return new LocalMediaLocation(legacyPath, displayName);
        }
        return null;
    }

    private String ensureWebp(String fileName) {
        if (fileName.endsWith(WEBP_EXTENSION)) {
            return fileName;
        }
        return fileName + WEBP_EXTENSION;
    }

    private String resolveContentType(String fileName, String fallbackContentType) {
        if (StringUtils.hasText(fallbackContentType)) {
            return fallbackContentType;
        }
        String detected = this.tika.detect(fileName);
        if (StringUtils.hasText(detected)) {
            return detected;
        }
        return "application/octet-stream";
    }

    private RangeRequest parseRange(String rangeHeader, long totalSize) {
        if (!StringUtils.hasText(rangeHeader) || totalSize <= 0) {
            return new RangeRequest(0, Math.max(totalSize - 1, 0));
        }
        if (!rangeHeader.startsWith("bytes=")) {
            return new RangeRequest(0, totalSize - 1);
        }

        String[] ranges = rangeHeader.substring("bytes=".length()).split("-");
        long start = ranges[0].isBlank() ? 0 : Long.parseLong(ranges[0]);
        long end;
        if (ranges.length > 1 && StringUtils.hasText(ranges[1])) {
            end = Long.parseLong(ranges[1]);
        } else {
            end = totalSize - 1;
        }

        start = Math.max(0, Math.min(start, totalSize - 1));
        end = Math.max(start, Math.min(end, totalSize - 1));
        return new RangeRequest(start, end);
    }

    private interface MediaLocation {

        long size() throws IOException;

        InputStream openStream() throws IOException;

        byte[] readRange(long start, long end) throws IOException;

        String displayName();
    }

    private record RangeRequest(long start, long end) {
    }

    private record ObjectMediaLocation(
            ObjectStorageService objectStorageService, String objectKey, long size, String displayName)
            implements MediaLocation {

        @Override
        public InputStream openStream() throws IOException {
            return this.objectStorageService.getObject(this.objectKey);
        }

        @Override
        public byte[] readRange(long start, long end) throws IOException {
            return this.objectStorageService.getObjectRange(this.objectKey, start, end);
        }
    }

    private record LocalMediaLocation(Path path, String displayName) implements MediaLocation {

        @Override
        public long size() throws IOException {
            return Files.size(this.path);
        }

        @Override
        public InputStream openStream() throws IOException {
            return Files.newInputStream(this.path);
        }

        @Override
        public byte[] readRange(long start, long end) throws IOException {
            long fileSize = Files.size(this.path);
            if (fileSize == 0 || start >= fileSize || end < start) {
                return new byte[0];
            }
            long boundedEnd = Math.min(end, fileSize - 1);
            long length = boundedEnd - start + 1;
            if (length > Integer.MAX_VALUE) {
                throw new IOException("Range too large");
            }

            byte[] result = new byte[(int) length];
            try (RandomAccessFile randomAccessFile = new RandomAccessFile(this.path.toFile(), "r")) {
                randomAccessFile.seek(start);
                randomAccessFile.readFully(result);
            }
            return result;
        }
    }
}
