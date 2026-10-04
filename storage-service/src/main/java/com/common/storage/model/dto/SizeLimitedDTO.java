package com.common.storage.model.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class SizeLimitedDTO implements Serializable {
    private Integer image;
    private Integer video;
    private Integer audio;
    private Integer file;
}
