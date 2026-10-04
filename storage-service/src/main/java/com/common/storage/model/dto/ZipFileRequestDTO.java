package com.common.storage.model.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ZipFileRequestDTO {
    @NotEmpty
    private List<String> files;
    private String name;
}
