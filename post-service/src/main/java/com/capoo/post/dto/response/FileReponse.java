package com.capoo.post.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FileReponse {
    // Id of the file in storage-service
    String id;
    // IMAGE, VIDEO or FILE
    String mediaType;
    String originalFileName;
}

