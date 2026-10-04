package com.common.storage.service.support;

import org.apache.commons.compress.utils.FileNameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Component
public class FilePathResolver {

    private static final String NON_ALPHANUMERIC_REGEX = "[^A-Za-z\\d]";
    private static final int MAX_RAW_FILE_NAME_LENGTH = 30;

    @Value("${storage.object.local.root:./home/media}")
    private String uploadPath;

    public String getUploadPath() {
        return this.uploadPath + "/";
    }

    public String getUploadPath(String ignoredUserPath) {
        return this.getUploadPath();
    }

    public String getThumbnailUploadPath() {
        return this.uploadPath + "/thumbnail/";
    }

    public String getThumbnailUploadPath(String ignoredUserPath) {
        return this.getThumbnailUploadPath();
    }

    public String getHlsUploadPath() {
        return this.uploadPath + "/hls/";
    }

    public String getHlsUploadPath(String ignoredUserPath) {
        return this.getHlsUploadPath();
    }

    public String getWebUploadPath() {
        return this.uploadPath + "/image/web/";
    }

    public String getWebUploadPath(String ignoredUserPath) {
        return this.getWebUploadPath();
    }

    public String getMobileUploadPath() {
        return this.uploadPath + "/image/mobile/";
    }

    public String getMobileUploadPath(String ignoredUserPath) {
        return this.getMobileUploadPath();
    }

    public String getVideoUploadPath(String quality) {
        return this.uploadPath + "/video/" + quality + "/";
    }

    public String getVideoUploadPath(String ignoredUserPath, String quality) {
        return this.getVideoUploadPath(quality);
    }

    public String createFileBaseName(String prefix, String originalFileName) {
        String fileNameRaw = FileNameUtils.getBaseName(originalFileName).replaceAll(NON_ALPHANUMERIC_REGEX, "");
        if (fileNameRaw.length() > MAX_RAW_FILE_NAME_LENGTH) {
            fileNameRaw = fileNameRaw.substring(0, MAX_RAW_FILE_NAME_LENGTH);
        }
        String dateTimePart = LocalDateTime.now().toString().replaceAll(NON_ALPHANUMERIC_REGEX, "");
        return prefix + "_" + dateTimePart + "_" + fileNameRaw;
    }

    public String getUserPathFromFilename(String filename) {
        return "";
    }

    public String getFileUriWithPrefix(String prefix) {
        Path root = Paths.get(this.uploadPath);
        if (!Files.exists(root)) {
            return null;
        }

        try {
            Optional<Path> matchedFile = Files.walk(root)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith(prefix))
                    .findFirst();
            return matchedFile.map(Path::toAbsolutePath).map(Path::toString).orElse(null);
        } catch (IOException e) {
            return null;
        }
    }
}
