package com.common.storage.helper;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Data
@Slf4j
@Component
public class SessionHelper {
    private static final UUID DEFAULT_USER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000000");
    private static final UUID ERROR_USER_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final RequesterHeaders requesterHeaders;
    private RestTemplate restTemplate;

    public SessionHelper(RequesterHeaders requesterHeaders) {
        this.requesterHeaders = requesterHeaders;
    }

    public UUID getCurrentUserID() {
        try {
            if (requesterHeaders != null && requesterHeaders.getUserId() != null) {
                return requesterHeaders.getUserId();
            }
            return DEFAULT_USER_ID;
        } catch (Exception ex) {
            return ERROR_USER_ID;
        }
    }

    public void setCurrentUserID(UUID userId) {
        requesterHeaders.setUserId(userId);
    }
}
