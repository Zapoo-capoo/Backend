package com.capoo.gateway.service;

import com.capoo.gateway.dto.ApiResponse;
import com.capoo.gateway.dto.request.IntrospectRequest;
import com.capoo.gateway.dto.response.IntrospectResponse;
import com.capoo.gateway.repository.IdentityClient;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class IdentityService {

    IdentityClient identityClient;
    public Mono<ApiResponse<IntrospectResponse>> introspectToken(String token) {
        return identityClient.introspect(
                IntrospectRequest.builder().token(token).build()
        );
    }

}
