package com.common.storage.service;

import com.common.storage.exception.MaxUploadSizeExceedException;
import com.common.storage.model.dto.BatchDeleteDTO;
import com.common.storage.model.dto.FileDTO;
import com.common.storage.model.dto.FileUpdateDTO;
import com.common.storage.model.dto.StreamingMediaDTO;
import com.common.storage.model.dto.ValidateFileRequestDTO;
import com.common.storage.model.dto.ValidateFileResponseDTO;
import com.common.storage.model.dto.ZipFileResponseDTO;
import com.common.storage.model.entity.jpa.File;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import ws.schild.jave.EncoderException;

import java.io.IOException;
import java.util.List;

public interface IFileService {
    FileDTO uploadFile(MultipartFile multipartFile, String description)
            throws IOException, MaxUploadSizeExceedException, EncoderException;
    List<FileDTO> uploadFiles(MultipartFile[] multipartFiles, String description)
            throws IOException, MaxUploadSizeExceedException, EncoderException;
    FileDTO getFileById(String fileId);
    void deleteFile(String fileId) throws IOException;
    void deleteFiles(BatchDeleteDTO fileIds) throws IOException;

    StreamingMediaDTO getWebFile(String filename, String rangeHeader) throws IOException;

    StreamingMediaDTO getThumbnailImage(String fileName) throws IOException;

    ResponseEntity<StreamingResponseBody> streamVideoV2(String fileName, String quality, String rangeHeader)
            throws IOException;

    StreamingMediaDTO downloadFile(String fileName, String rangeHeader) throws IOException;
//
//

}
