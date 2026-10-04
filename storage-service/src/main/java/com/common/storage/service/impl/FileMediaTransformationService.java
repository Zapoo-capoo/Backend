package com.common.storage.service.impl;

import com.common.storage.exception.NotFoundException;
import com.common.storage.exception.ObjectStorageNotFoundException;
import com.common.storage.exception.InvalidFileTypeException;
import com.common.storage.model.entity.jpa.File;
import com.common.storage.repository.FileRepository;
import com.common.storage.service.IFileMediaTransformationService;
import com.common.storage.service.support.FilePathResolver;
import com.common.storage.service.support.FileSecurityValidator;
import com.common.storage.storage.ObjectKeyResolver;
import com.common.storage.storage.ObjectStorageService;
import com.common.storage.storage.StorageObjectProperties;
import com.common.storage.util.FileUploadUtil;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifIFD0Directory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.io.IOUtils;
import org.imgscalr.Scalr;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalTime;
import java.util.Base64;
import java.util.Comparator;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileMediaTransformationService implements IFileMediaTransformationService {

    private static final String WEBP_EXTENSION = ".webp";
    private static final int WIDTH_RESIZE_WEB_IMAGE = 1500;
    private static final int HEIGHT_RESIZE_WEB_IMAGE = 1000;
    private static final int WIDTH_THUMBNAIL_SIZE = 1200;
    private static final int HEIGHT_THUMBNAIL_SIZE = 800;

    private final FileRepository fileRepository;
    private final FileUploadUtil fileUploadUtil;
    private final FilePathResolver filePathResolver;
    private final FileSecurityValidator fileSecurityValidator;
    private final ObjectStorageService objectStorageService;
    private final ObjectKeyResolver objectKeyResolver;
    private final StorageObjectProperties storageObjectProperties;

    @Value("${media.ffmpeg-path:ffmpeg}")
    private String ffmpegPath;

    @Value("${storage.video.qualities:240, 360, 720, 1080}")
    private String videoQualities;

    @Override
    public void convertWebp(String fileName)
            throws IOException, ImageProcessingException, MetadataException {
        if (fileName.toLowerCase().endsWith(".gif")) {
            return;
        }

        String fileId = FileNameUtils.getBaseName(fileName);
        String rawExtension = FileNameUtils.getExtension(fileName);
        String extension = StringUtils.hasText(rawExtension) ? "." + rawExtension : "";

        Path workingDirectory = Files.createTempDirectory("storage-image-");
        try {
            Path sourcePath = this.downloadOriginalToPath(fileId, extension, workingDirectory.resolve(fileName));

            try {
                this.fileSecurityValidator.checkXXE(sourcePath);
            } catch (Exception ex) {
                log.error("CHECK XXE ERROR", ex);
            }

            java.io.File sourceFile = sourcePath.toFile();
            if (sourceFile.isFile()) {
                String fileNameContext = fileId + WEBP_EXTENSION;
                Path webFilePath = this.toWebWebp(sourceFile, fileNameContext, workingDirectory);
                Path mobileFilePath = this.toMobileWebp(sourceFile, fileNameContext, workingDirectory);
                Path thumbnailFilePath = this.createThumbnail(sourceFile, fileNameContext, workingDirectory);

                this.uploadPathToObjectStorage(this.objectKeyResolver.web(fileId), webFilePath, "image/webp");
                this.uploadPathToObjectStorage(this.objectKeyResolver.mobile(fileId), mobileFilePath, "image/webp");
                this.uploadPathToObjectStorage(this.objectKeyResolver.thumbnail(fileId), thumbnailFilePath, "image/webp");
            }
        } finally {
            this.cleanupWorkingDirectory(workingDirectory);
        }
    }

    @Override
    public void createThumbnailForVideo(Path inputFilePath, String fileId) {
        Path workingDirectory = null;
        try {
            workingDirectory = Files.createTempDirectory("storage-video-thumbnail-");
            Path sourcePath = inputFilePath;

            if (sourcePath == null || !Files.exists(sourcePath)) {
                File file = this.fileRepository.findById(fileId).orElse(null);
                if (file == null) {
                    return;
                }
                sourcePath = this.downloadOriginalToPath(
                        fileId, file.getExtension(),
                        workingDirectory.resolve(fileId + file.getExtension()));
            }

            Path outputThumbnailPath = workingDirectory.resolve(fileId + WEBP_EXTENSION);

            ProcessBuilder builder = new ProcessBuilder(
                    this.ffmpegPath,
                    "-i", sourcePath.toString(),
                    "-ss", "00:00:01",
                    "-vframes", "1",
                    "-q:v", "2",
                    "-threads", "2",
                    outputThumbnailPath.toString()
            );
            builder.redirectErrorStream(true);

            log.info("Executing thumbnail generation command: {}", String.join(" ", builder.command()));

            Process process = builder.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                this.uploadPathToObjectStorage(
                        this.objectKeyResolver.thumbnail(fileId), outputThumbnailPath, "image/webp");
                log.info("Thumbnail created successfully: {}", outputThumbnailPath);
            } else {
                log.error("Error while generating thumbnail. Command output:\n{}", output);
            }
        } catch (Exception ex) {
            log.error("Error when trying to generate the thumbnail: ", ex);
        } finally {
            this.cleanupWorkingDirectory(workingDirectory);
        }
    }


    @Override
    public void transcodeVideoQualities(Path inputFilePath, String fileId) {
        Path workingDirectory = null;
        try {
            workingDirectory = Files.createTempDirectory("storage-video-transcode-");
            Path sourcePath = inputFilePath;

            if (sourcePath == null || !Files.exists(sourcePath)) {
                File file = this.fileRepository.findById(fileId).orElse(null);
                if (file == null) {
                    return;
                }
                sourcePath = this.downloadOriginalToPath(
                        fileId, file.getExtension(),
                        workingDirectory.resolve(fileId + file.getExtension()));
            }

            for (String rawQuality : this.videoQualities.split(",")) {
                String quality = rawQuality.trim();
                if (!quality.isEmpty()) {
                    this.transcodeSingleQuality(sourcePath, fileId, quality, workingDirectory);
                }
            }
        } catch (Exception ex) {
            log.error("Error when transcoding video qualities for {}: ", fileId, ex);
        } finally {
            this.cleanupWorkingDirectory(workingDirectory);
        }
    }

    private void transcodeSingleQuality(Path sourcePath, String fileId, String quality, Path workingDirectory) {
        try {
            int height = Integer.parseInt(quality.replaceAll("[^0-9]", ""));
            Path outputPath = workingDirectory.resolve(fileId + "_" + quality + ".mp4");

            ProcessBuilder builder = new ProcessBuilder(
                    this.ffmpegPath,
                    "-y",
                    "-i", sourcePath.toString(),
                    "-vf", "scale=-2:" + height,
                    "-c:v", "libx264",
                    "-preset", "veryfast",
                    "-crf", "23",
                    "-c:a", "aac",
                    "-b:a", "128k",
                    "-movflags", "+faststart",
                    "-threads", "2",
                    outputPath.toString()
            );
            builder.redirectErrorStream(true);
            log.info("Executing video transcode command: {}", String.join(" ", builder.command()));

            Process process = builder.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0 && Files.exists(outputPath)) {
                this.uploadPathToObjectStorage(
                        this.objectKeyResolver.video(fileId, quality, ".mp4"), outputPath, "video/mp4");
                log.info("Transcoded video {} to quality {}", fileId, quality);
            } else {
                log.error("Transcode failed for {} quality {}. Command output:\n{}", fileId, quality, output);
            }
        } catch (Exception ex) {
            log.error("Error transcoding {} to quality {}: ", fileId, quality, ex);
        }
    }

    private Path toWebWebp(java.io.File file, String fileNameContext, Path workingDirectory) throws IOException {
        Path convertPath = workingDirectory.resolve("image/web");
        if (!Files.exists(convertPath)) {
            Files.createDirectories(convertPath);
        }
        Path filePath = convertPath.resolve(fileNameContext);
        BufferedImage image;
        try {
            image = this.fileUploadUtil.handleBufferedImage(file);
        } catch (Exception ex) {
            log.error("{}: Exception data: {}", LocalTime.now(), fileNameContext, ex);
            throw new InvalidFileTypeException();
        }
        this.resizeAndConvertImage(
                String.valueOf(Path.of(file.getPath())), String.valueOf(filePath), image,
                WIDTH_RESIZE_WEB_IMAGE, HEIGHT_RESIZE_WEB_IMAGE);
        return filePath;
    }

    private Path toMobileWebp(java.io.File file, String fileNameContext, Path workingDirectory)
            throws IOException, ImageProcessingException, MetadataException {
        Path uploadPath = workingDirectory.resolve("image/mobile");
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        BufferedImage img;
        try {
            img = this.fileUploadUtil.handleBufferedImage(file);
        } catch (Exception ex) {
            throw new InvalidFileTypeException();
        }
        BufferedImage thumbImg = Scalr.resize(img, Scalr.Method.AUTOMATIC, Scalr.Mode.AUTOMATIC, 320, Scalr.OP_ANTIALIAS);
        Path filePath = uploadPath.resolve(fileNameContext);
        ImageIO.write(thumbImg, "webp", filePath.toFile());
        return filePath;
    }

    private Path createThumbnail(java.io.File file, String originalFileName, Path workingDirectory) throws IOException {
        Path uploadThumbnailPath = workingDirectory.resolve("thumbnail");
        if (!Files.exists(uploadThumbnailPath)) {
            Files.createDirectories(uploadThumbnailPath);
        }
        BufferedImage img;
        try {
            img = this.fileUploadUtil.handleBufferedImage(file);
        } catch (Exception ex) {
            throw new InvalidFileTypeException();
        }
        Path filePath = uploadThumbnailPath.resolve(originalFileName);
        this.resizeAndConvertImage(
                String.valueOf(Path.of(file.getPath())), String.valueOf(filePath), img,
                WIDTH_THUMBNAIL_SIZE, HEIGHT_THUMBNAIL_SIZE);
        return filePath;
    }

    private void resizeAndConvertImage(
            String sourcePath, String targetResizedPath, BufferedImage inputImage,
            int widthResize, int heightResize) {
        log.info("Convert file: {}", sourcePath);
        try {
            java.io.File file = new java.io.File(sourcePath);
            boolean isSvg = "image/svg+xml".equals(Files.probeContentType(file.toPath()));
            int originalWidth = inputImage.getWidth();
            int originalHeight = inputImage.getHeight();
            double aspectRatio = (double) originalWidth / originalHeight;
            int newWidth;
            int newHeight;
            if (aspectRatio > (double) widthResize / heightResize) {
                newWidth = widthResize;
                newHeight = (int) (widthResize / aspectRatio);
            } else {
                newHeight = heightResize;
                newWidth = (int) (heightResize * aspectRatio);
            }
            int rotate = 0;
            try {
                ExifIFD0Directory firstDir = ImageMetadataReader.readMetadata(file)
                        .getFirstDirectoryOfType(ExifIFD0Directory.class);
                if (firstDir != null) {
                    int rotateRaw = firstDir.getInt(ExifIFD0Directory.TAG_ORIENTATION);
                    switch (rotateRaw) {
                        case 3 -> rotate = 180;
                        case 5 -> rotate = -90;
                        case 6 -> rotate = 90;
                        case 7, 8 -> rotate = 270;
                        default -> rotate = 0;
                    }
                }
            } catch (Exception ex) {
                log.error("Exception reading EXIF data", ex);
            }
            Image scaledImage = inputImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
            String outputType = isSvg ? "png" : "webp";
            BufferedImage outputImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
            outputImage.createGraphics().drawImage(scaledImage, 0, 0, null);
            ImageIO.write(this.rotate(outputImage, rotate), outputType, new java.io.File(targetResizedPath));
        } catch (Exception ex) {
            log.error("Exception resizing image: {}", sourcePath, ex);
        }
    }

    private BufferedImage rotate(BufferedImage bimg, double angle) {
        double sin = Math.abs(Math.sin(Math.toRadians(angle)));
        double cos = Math.abs(Math.cos(Math.toRadians(angle)));
        int w = bimg.getWidth();
        int h = bimg.getHeight();
        int neww = (int) Math.floor(w * cos + h * sin);
        int newh = (int) Math.floor(h * cos + w * sin);
        BufferedImage rotated = new BufferedImage(neww, newh, bimg.getType());
        Graphics2D graphic = rotated.createGraphics();
        graphic.translate((neww - w) / 2D, (newh - h) / 2D);
        graphic.rotate(Math.toRadians(angle), w / 2D, h / 2D);
        graphic.drawRenderedImage(bimg, null);
        graphic.dispose();
        return rotated;
    }

    private Path downloadOriginalToPath(String fileId, String extension, Path localPath) throws IOException {
        String objectKey = this.objectKeyResolver.original(fileId, extension);
        try (InputStream inputStream = this.objectStorageService.getObject(objectKey)) {
            Path parent = localPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.copy(inputStream, localPath, StandardCopyOption.REPLACE_EXISTING);
            return localPath;
        } catch (ObjectStorageNotFoundException notFoundException) {
            if (!this.storageObjectProperties.isReadFallbackLocalEnabled()) {
                throw notFoundException;
            }
            Path legacyPath = Paths.get(this.filePathResolver.getUploadPath(), fileId + extension);
            if (!Files.exists(legacyPath)) {
                throw notFoundException;
            }
            Path parent = localPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.copy(legacyPath, localPath, StandardCopyOption.REPLACE_EXISTING);
            return localPath;
        }
    }

    private void uploadPathToObjectStorage(String objectKey, Path sourcePath, String contentType) throws IOException {
        String resolvedType = contentType;
        if (!StringUtils.hasText(resolvedType)) {
            resolvedType = Files.probeContentType(sourcePath);
        }
        try (InputStream inputStream = Files.newInputStream(sourcePath)) {
            this.objectStorageService.putObject(objectKey, inputStream, resolvedType, Files.size(sourcePath));
        }
    }

    private void cleanupWorkingDirectory(Path workingDirectory) {
        if (workingDirectory == null || !Files.exists(workingDirectory)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(workingDirectory)) {
            stream.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ex) {
                    log.debug("Cleanup failed for path: {}", path, ex);
                }
            });
        } catch (IOException ex) {
            log.debug("Cleanup working directory failed: {}", workingDirectory, ex);
        }
    }
}
