package com.common.storage.service;

import com.common.storage.exception.MaxUploadSizeExceedException;
import com.common.storage.model.dto.BatchDeleteDTO;
import com.common.storage.model.dto.FileDTO;
import org.springframework.web.multipart.MultipartFile;
import ws.schild.jave.EncoderException;

import java.io.IOException;
import java.util.List;

public interface IFileCommandService {

    FileDTO uploadFile(MultipartFile multipartFile, String description)
            throws IOException, MaxUploadSizeExceedException, EncoderException;
    List<FileDTO> uploadFiles(MultipartFile[] multipartFiles, String description)
            throws IOException, MaxUploadSizeExceedException, EncoderException;
    void deleteFile(String fileId) throws IOException;
    void deleteFiles(BatchDeleteDTO fileIds) throws IOException;
}
