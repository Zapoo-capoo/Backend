package com.common.storage.model.dto;

import com.common.storage.model.entity.jpa.File;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileDTO {
    private String id;
    private String extension;
    private String original_name;
    private String type;
    private Long size;
    private String description;

    @Deprecated
    private String signature;
    @Deprecated
    private List<String> qualities;

    public static FileDTO fromEntity(File file) {
        return FileDTO.builder()
                .id(file.getId())
                .extension(file.getExtension())
                .original_name(file.getOriginalName())
                .type(file.getType())
                .size(file.getSize())
                .description(file.getDescription())
                .build();
    }
}
