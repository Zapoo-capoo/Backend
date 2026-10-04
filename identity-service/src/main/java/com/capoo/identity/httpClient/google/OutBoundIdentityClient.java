package com.capoo.identity.httpClient.google;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;

import com.capoo.identity.dto.request.ExchangeTokenRequest;
import com.capoo.identity.dto.response.ExchangeTokenResponse;

import feign.QueryMap;

@FeignClient(name = "outbound-identity-service", url = "https://oauth2.googleapis.com")
public interface OutBoundIdentityClient {
    @PostMapping(value = "/token", produces = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    ExchangeTokenResponse exchangeToken(@QueryMap ExchangeTokenRequest request);
}
