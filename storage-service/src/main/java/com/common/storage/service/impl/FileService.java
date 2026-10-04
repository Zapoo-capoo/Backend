package com.common.storage.service.impl;

import com.common.storage.exception.MaxUploadSizeExceedException;
import com.common.storage.model.dto.*;
import com.common.storage.service.IFileService;
import com.common.storage.service.IFileCommandService;
import com.common.storage.service.IFileQueryService;
import com.common.storage.service.IFileMediaDeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import ws.schild.jave.EncoderException;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FileService implements IFileService {

    private final IFileCommandService fileCommandService;
    private final IFileQueryService fileQueryService;
    private final IFileMediaDeliveryService fileMediaDeliveryService;

    @Override
    public FileDTO uploadFile(MultipartFile multipartFile, String description)
            throws IOException, MaxUploadSizeExceedException, EncoderException {
        return this.fileCommandService.uploadFile(multipartFile, description);
    }

    @Override
    public List<FileDTO> uploadFiles(MultipartFile[] multipartFiles, String description)
            throws IOException, MaxUploadSizeExceedException, EncoderException {
        return this.fileCommandService.uploadFiles(multipartFiles, description);
    }
    @Override
    public FileDTO getFileById(String fileId) {
        return this.fileQueryService.getFileById(fileId);
    }
    @Override
    public void deleteFile(String fileId) throws IOException {
        this.fileCommandService.deleteFile(fileId);
    }
    @Override
    public void deleteFiles(BatchDeleteDTO fileIds) throws IOException {
        this.fileCommandService.deleteFiles(fileIds);
    }

    @Override
    public StreamingMediaDTO getWebFile(String filename, String rangeHeader) throws IOException {
        return this.fileMediaDeliveryService.getWebFile(filename, rangeHeader);
    }
    @Override
    public StreamingMediaDTO getThumbnailImage(String fileName) throws IOException {
        return this.fileMediaDeliveryService.getThumbnailImage(fileName);
    }
    @Override
    public StreamingMediaDTO downloadFile(String fileName, String rangeHeader) throws IOException {
        return this.fileMediaDeliveryService.downloadFile(fileName, rangeHeader);
    }


    @Override
    public ResponseEntity<StreamingResponseBody> streamVideoV2(String fileName, String quality, String rangeHeader)
            throws IOException {
        return this.fileMediaDeliveryService.streamVideoV2(fileName, quality, rangeHeader);
    }



}
