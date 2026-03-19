package com.capoo.identity.repository.httpClient.google;

import com.capoo.identity.dto.request.ExchangeTokenRequest;
import com.capoo.identity.dto.response.ExchangeTokenResponse;
import feign.QueryMap;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name="outbound-identity-service", url="https://oauth2.googleapis.com")
public interface OutBoundIdentityClient {
    @PostMapping(value="/auth/introspect", produces = MediaType.APPLICATION_JSON_VALUE)
    ExchangeTokenResponse exchangeToken(@QueryMap ExchangeTokenRequest request);
}
