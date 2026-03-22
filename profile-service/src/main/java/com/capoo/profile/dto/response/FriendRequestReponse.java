package com.capoo.profile.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class FriendRequestReponse {
    private String fromId;
    private String toId;
    private String status; // SENT
}