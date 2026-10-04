package com.capoo.chat.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatMessageRequest {
    @NotBlank
    String conversationId;

    String message;

    /**
     * Ids of files already uploaded to storage-service (at most 5, images or videos). The client uploads the files
     * first and sends their ids here.
     */
    List<String> fileIds;
}
