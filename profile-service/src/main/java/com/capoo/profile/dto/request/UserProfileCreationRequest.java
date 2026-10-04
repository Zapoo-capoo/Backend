package com.capoo.profile.dto.request;

import java.time.LocalDate;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserProfileCreationRequest {
    String userId;
    String username;
    String email;
    String firstName;
    String lastName;
    LocalDate dob;
    String avatar;
}
