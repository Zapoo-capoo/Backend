package com.capoo.chat.service;

import com.capoo.chat.dto.request.IntrospectRequest;
import com.capoo.chat.dto.response.IntrospectResponse;

public interface IdentityService {
    IntrospectResponse introspectToken(IntrospectRequest request);
}
