package com.common.storage.helper;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
@Component
@RequestScope
public class RequesterHeaders implements Serializable {
    private UUID userId;
    private String messageId;
    private String language;
}
