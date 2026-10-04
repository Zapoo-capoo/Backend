package com.common.storage.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FileUpdateDTO {
    @NotNull
    private String description;
}
