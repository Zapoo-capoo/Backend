package com.common.storage.service;

import com.common.storage.model.dto.FileDTO;
import com.common.storage.model.dto.FileUpdateDTO;
import com.common.storage.model.dto.ValidateFileRequestDTO;
import com.common.storage.model.dto.ValidateFileResponseDTO;

public interface IFileQueryService {

    FileDTO getFileById(String fileId);

    FileDTO updateFile(String fileId, FileUpdateDTO fileUpdateDTO);

    ValidateFileResponseDTO[] validateFile(ValidateFileRequestDTO[] fileRequests);
}
