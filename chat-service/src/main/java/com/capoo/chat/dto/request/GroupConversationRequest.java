package com.capoo.chat.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GroupConversationRequest {
    @NotBlank(message = "GROUP_NAME_REQUIRED")
    @Size(max = 100, message = "GROUP_NAME_TOO_LONG")
    String name;

    /** userIds of the members to invite, the creator is added automatically and must not be listed. */
    @NotEmpty(message = "GROUP_MEMBERS_REQUIRED")
    List<String> participantIds;
}
