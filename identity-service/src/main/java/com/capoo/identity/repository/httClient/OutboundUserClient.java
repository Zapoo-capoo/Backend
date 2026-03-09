package com.capoo.identity.repository.httClient;

import com.capoo.identity.dto.response.ExchangeTokenResponse;
import com.capoo.identity.dto.response.GoogleUserInfoResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name="outbound-user-client", url="https://www.googleapis.com")
public interface OutboundUserClient {
    @GetMapping(value="/oauth2/v1/userinfo")
    GoogleUserInfoResponse getUserInfo(
            @RequestParam("alt") String alt,
            @RequestParam("access_token") String accessToken);

}
