package com.capoo.chat.repository.httpclient;

import com.capoo.chat.configuration.AuthenticationRequestInterceptor;
import com.capoo.chat.dto.ApiResponse;
import com.capoo.chat.dto.request.IntrospectRequest;
import com.capoo.chat.dto.response.IntrospectResponse;
import com.capoo.chat.dto.response.UserProfileResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "IdentityClient", url = "${app.services.identity.url}",
        configuration = { AuthenticationRequestInterceptor.class })
public interface IdentityClient {
    @PostMapping("/auth/introspect")
    ApiResponse<IntrospectResponse> introspectToken(@RequestBody  IntrospectRequest request);

}
