package com.capoo.chat.repository.httpclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.capoo.chat.dto.request.IntrospectRequest;
import com.capoo.chat.dto.response.IntrospectResponse;
import com.capoo.dto.ApiResponse;

@FeignClient(name = "IdentityClient", url = "${app.services.identity.url}")
public interface IdentityClient {
    @PostMapping("/auth/introspect")
    ApiResponse<IntrospectResponse> introspectToken(@RequestBody IntrospectRequest request);
}
