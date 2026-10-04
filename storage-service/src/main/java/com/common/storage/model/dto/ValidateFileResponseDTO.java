package com.common.storage.model.dto;

import lombok.Data;

@Data
public class ValidateFileResponseDTO {

    private String id;
    private String signature;
    private boolean existed = false;
}
