package com.capoo.chat.entity;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ParticipantInfo {
    String userId;
    String username;
    String firstName;
    String lastName;
    String avatar;

    /**
     * Only meaningful for the participants of a conversation: false while there is a message this participant has not
     * opened yet. Null (and left out of the JSON) where ParticipantInfo is used as a message sender, and on
     * conversations created before this field existed.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    Boolean hasSeen;
}
