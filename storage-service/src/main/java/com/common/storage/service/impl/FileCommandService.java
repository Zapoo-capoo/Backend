package com.common.storage.service.impl;

import com.common.storage.constant.SUPPORT_FILE_TYPE;
import com.common.storage.exception.MaxUploadSizeExceedException;
import com.common.storage.exception.NotFoundException;
import com.common.storage.helper.SessionHelper;
import com.common.storage.model.dto.BatchDeleteDTO;
import com.common.storage.model.dto.FileDTO;
import com.common.storage.model.entity.jpa.File;
import com.common.storage.repository.FileRepository;
import com.common.storage.service.IFileCommandService;
import com.common.storage.service.IFileMediaTransformationService;
import com.common.storage.service.support.FilePathResolver;
import com.common.storage.service.support.FileSecurityValidator;
import com.common.storage.storage.ObjectKeyResolver;
import com.common.storage.storage.ObjectStorageService;
import com.common.storage.util.FileUploadUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.FileNameUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import ws.schild.jave.EncoderException;
import ws.schild.jave.MultimediaObject;

import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileCommandService implements IFileCommandService {

    private final FileRepository fileRepository;
    private final FileUploadUtil fileUploadUtil;
    private final FilePathResolver filePathResolver;
    private final FileSecurityValidator fileSecurityValidator;
    private final IFileMediaTransformationService fileMediaTransformationService;
    private final ObjectStorageService objectStorageService;
    private final ObjectKeyResolver objectKeyResolver;
    private final SessionHelper sessionHelper;

    @Qualifier("mediaTaskExecutor")
    private final TaskExecutor mediaTaskExecutor;

    @Value("${storage.video.duration:600000}")
    private long durationMilliseconds;

    @Value("${storage.prefix:file}")
    private String prefix;

    @Override
    public FileDTO uploadFile(MultipartFile multipartFile, String description)
            throws IOException, MaxUploadSizeExceedException, EncoderException {

        String fileType = this.fileUploadUtil.validateUploadingFile(multipartFile);
        String originalFileName = StringUtils.cleanPath(Objects.requireNonNull(multipartFile.getOriginalFilename()));
        String fileBaseName = this.filePathResolver.createFileBaseName(this.prefix, originalFileName);
        String extension = this.resolveExtension(originalFileName);
        String fileName = fileBaseName + extension;

        Path workingDirectory = Files.createTempDirectory("storage-upload-");
        Path filePath = workingDirectory.resolve(fileName);

        try {
            //copy file to temp storage
            try (InputStream inputStream = multipartFile.getInputStream()) {
                Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            }
            //XML External Entity
            if (this.fileSecurityValidator.checkXXE(filePath)) {
                throw new IOException("File not valid");
            }
            //Check video duration(video type)
            if (SUPPORT_FILE_TYPE.Video.MIME_TYPES.contains(fileType)) {
                MultimediaObject media = new MultimediaObject(filePath.toFile());
                long durationVideo = media.getInfo().getDuration();
                if (durationVideo > this.durationMilliseconds) {
                    throw new MaxUploadSizeExceedException(
                            "Video file size should not exceed: " + this.durationMilliseconds / 1000 + " seconds");
                }
            }

            this.uploadPathToObjectStorage(this.objectKeyResolver.original(fileBaseName, extension), filePath, fileType);

            File file = new File();
            file.setId(fileBaseName);
            file.setOriginalName(originalFileName);
            file.setExtension(extension);
            file.setType(fileType);
            file.setSize(multipartFile.getSize());
            file.setDescription(description);
            file.setCreatedBy(this.sessionHelper.getCurrentUserID().toString());
            file.setUpdatedBy(this.sessionHelper.getCurrentUserID().toString());
            file.setCreatedAt(LocalDateTime.now());
            file.setUpdatedAt(LocalDateTime.now());
            this.fileRepository.save(file);

            this.dispatchMediaPostProcessing(fileType, fileBaseName, extension, filePath, workingDirectory);
            return FileDTO.fromEntity(file);
        } catch (IOException | MaxUploadSizeExceedException | EncoderException | RuntimeException exception) {
            this.cleanupWorkingDirectory(workingDirectory);
            throw exception;
        }
    }
    @Override
    public List<FileDTO> uploadFiles(MultipartFile[] multipartFiles, String description)
            throws IOException, MaxUploadSizeExceedException, EncoderException {
        List<FileDTO> results = new ArrayList<>();
        for (MultipartFile multipartFile : multipartFiles) {
            results.add(this.uploadFile(multipartFile, description));
        }
        return results;
    }

    @Override
    public void deleteFile(String fileId) throws IOException {
        File file = this.fileRepository.findById(fileId)
                .orElseThrow(() -> new NotFoundException("File not found: " + fileId));
        this.deleteObjectStorageVariants(file);
        this.fileRepository.deleteById(fileId);
    }
    @Override
    public void deleteFiles(BatchDeleteDTO fileIds) throws IOException {
        for (String fileId : fileIds.getFile_ids()) {
            this.deleteFile(fileId);
        }
    }

    private void uploadPathToObjectStorage(String objectKey, Path sourcePath, String contentType) throws IOException {
        try (InputStream inputStream = Files.newInputStream(sourcePath)) {
            this.objectStorageService.putObject(objectKey, inputStream, contentType, Files.size(sourcePath));
        }
    }

    private void dispatchMediaPostProcessing(
            String fileType, String fileId, String extension, Path filePath, Path workingDirectory) {
        if (SUPPORT_FILE_TYPE.Image.MIME_TYPES.contains(fileType)) {
            this.mediaTaskExecutor.execute(() -> {
                try {
                    this.fileMediaTransformationService.convertWebp(fileId + extension);
                } catch (Exception exception) {
                    log.error("Image conversion failed for {}", fileId, exception);
                } finally {
                    this.cleanupWorkingDirectory(workingDirectory);
                }
            });
            return;
        }

        if (SUPPORT_FILE_TYPE.Video.MIME_TYPES.contains(fileType)) {
            this.mediaTaskExecutor.execute(() -> {
                try {
                    this.fileMediaTransformationService.createThumbnailForVideo(filePath, fileId);
                    this.fileMediaTransformationService.transcodeVideoQualities(filePath, fileId);
                } catch (Exception exception) {
                    log.error("Video post-processing failed for {}", fileId, exception);
                } finally {
                    this.cleanupWorkingDirectory(workingDirectory);
                }
            });
            return;
        }

        this.cleanupWorkingDirectory(workingDirectory);
    }

    private String resolveExtension(String originalFileName) {
        String extension = FileNameUtils.getExtension(originalFileName);
        if (!StringUtils.hasText(extension)) {
            return "";
        }
        return "." + extension;
    }


    private void cleanupWorkingDirectory(Path workingDirectory) {
        if (workingDirectory == null || !Files.exists(workingDirectory)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(workingDirectory)) {
            stream.sorted((left, right) -> right.compareTo(left))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException exception) {
                            log.debug("Cleanup failed for path: {}", path, exception);
                        }
                    });
        } catch (IOException exception) {
            log.debug("Cleanup working directory failed: {}", workingDirectory, exception);
        }
    }

    private void deleteObjectStorageVariants(File file) throws IOException {
        String fileId = file.getId();
        String extension = file.getExtension();
        String fileType = file.getType();

        this.deleteObjectIfExists(this.objectKeyResolver.original(fileId, extension));

        if (SUPPORT_FILE_TYPE.Image.MIME_TYPES.contains(fileType)) {
            this.deleteObjectIfExists(this.objectKeyResolver.web(fileId));
            this.deleteObjectIfExists(this.objectKeyResolver.mobile(fileId));
            this.deleteObjectIfExists(this.objectKeyResolver.thumbnail(fileId));
            return;
        }

        if (SUPPORT_FILE_TYPE.Video.MIME_TYPES.contains(fileType)) {
            this.deleteObjectIfExists(this.objectKeyResolver.thumbnail(fileId));
        }
    }
    private void deleteObjectIfExists(String objectKey) throws IOException {
        if (this.objectStorageService.exists(objectKey)) {
            this.objectStorageService.delete(objectKey);
        }
    }
}
