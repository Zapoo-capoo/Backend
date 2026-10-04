package com.capoo.chat.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AddParticipantsRequest {
    /** userIds of the people to add to the group. */
    @NotEmpty(message = "GROUP_MEMBERS_REQUIRED")
    List<String> participantIds;
}
