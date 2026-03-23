package com.capoo.event.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProfileCreatedEvent {
    String userId;
    String status;//SUCCESS,FAILED
    String  username;
    String email;
}
