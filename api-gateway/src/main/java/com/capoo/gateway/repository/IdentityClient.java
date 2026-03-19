package com.capoo.gateway.repository;

import com.capoo.gateway.dto.ApiResponse;
import com.capoo.gateway.dto.request.IntrospectRequest;
import com.capoo.gateway.dto.response.IntrospectResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.PostExchange;
import reactor.core.publisher.Mono;
public interface IdentityClient {
    @PostExchange(url="/auth/introspect")
    Mono<ApiResponse<IntrospectResponse>> introspect(@RequestBody IntrospectRequest request);
}
