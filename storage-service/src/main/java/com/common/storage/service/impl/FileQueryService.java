package com.common.storage.service.impl;

import com.common.storage.constant.EErrorCodes;
import com.common.storage.exception.NotFoundException;
import com.common.storage.model.dto.FileDTO;
import com.common.storage.model.dto.FileUpdateDTO;
import com.common.storage.model.dto.ValidateFileRequestDTO;
import com.common.storage.model.dto.ValidateFileResponseDTO;
import com.common.storage.model.entity.jpa.File;
import com.common.storage.repository.FileRepository;
import com.common.storage.service.IFileQueryService;
import com.common.storage.util.MD5Util;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FileQueryService implements IFileQueryService {

    private final FileRepository fileRepository;
    private final MD5Util md5Util;

    @Override
    public FileDTO getFileById(String fileId) {
        File file = this.fileRepository.findById(fileId)
                .orElseThrow(() -> new NotFoundException(EErrorCodes.FILE_NOT_FOUND.name()));
        return this.toDto(file);
    }

    @Override
    public FileDTO updateFile(String fileId, FileUpdateDTO fileUpdateDTO) {
        File file = this.fileRepository.findById(fileId)
                .orElseThrow(() -> new NotFoundException(EErrorCodes.FILE_NOT_FOUND.name()));
        file.setDescription(fileUpdateDTO.getDescription() == null ? "" : fileUpdateDTO.getDescription());
        this.fileRepository.save(file);
        return this.toDto(file);
    }

    @Override
    public ValidateFileResponseDTO[] validateFile(ValidateFileRequestDTO[] fileRequests) {
        if (fileRequests == null || fileRequests.length == 0) {
            return new ValidateFileResponseDTO[0];
        }

        ValidateFileResponseDTO[] responses = new ValidateFileResponseDTO[fileRequests.length];
        for (int i = 0; i < fileRequests.length; i++) {
            ValidateFileRequestDTO request = fileRequests[i];
            ValidateFileResponseDTO response = new ValidateFileResponseDTO();
            response.setId(request.getId());
            response.setSignature(request.getSignature());

            if (request.getId() != null && request.getSignature() != null) {
                this.fileRepository.findById(request.getId()).ifPresent(file -> {
                    String signature = this.md5Util.encode(request.getId());
                    if (signature.equals(request.getSignature())) {
                        response.setExisted(true);
                    }
                });
            }

            responses[i] = response;
        }
        return responses;
    }

    private FileDTO toDto(File file) {
        FileDTO fileDTO = new FileDTO();
        fileDTO.setId(file.getId());
        fileDTO.setExtension(file.getExtension());
        fileDTO.setOriginal_name(file.getOriginalName());
        fileDTO.setType(file.getType());
        fileDTO.setSize(file.getSize());
        fileDTO.setDescription(file.getDescription());
        return fileDTO;
    }
}
