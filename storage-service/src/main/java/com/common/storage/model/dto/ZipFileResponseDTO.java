package com.common.storage.model.dto;

import lombok.Builder;
import lombok.Data;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;

@Data
@Builder
public class ZipFileResponseDTO {
    private HttpHeaders headers;
    private InputStreamResource body;
}
