package com.capoo.profile.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateParticipantRequest {
    String userId;
    String username;
    String firstName;
    String lastName;
    String avatar;
}

