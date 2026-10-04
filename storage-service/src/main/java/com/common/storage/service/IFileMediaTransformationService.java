package com.common.storage.service;

import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.MetadataException;

import java.io.IOException;
import java.nio.file.Path;

public interface IFileMediaTransformationService {

    void convertWebp(String fileName) throws IOException, ImageProcessingException, MetadataException;

    void createThumbnailForVideo(Path inputFilePath, String fileId);

    void transcodeVideoQualities(Path inputFilePath, String fileId);

}
