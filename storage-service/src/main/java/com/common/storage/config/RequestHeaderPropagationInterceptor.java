package com.common.storage.config;

import com.common.storage.helper.RequesterHeaders;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RequestHeaderPropagationInterceptor implements ClientHttpRequestInterceptor {

    private final RequesterHeaders requesterHeaders;

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution) throws IOException {
        if (this.requesterHeaders.getUserId() != null) {
            request.getHeaders().set("userId", this.requesterHeaders.getUserId().toString());
        }
        if (this.requesterHeaders.getMessageId() != null
                && !this.requesterHeaders.getMessageId().isBlank()) {
            request.getHeaders().set("messageId", this.requesterHeaders.getMessageId());
        }
        return execution.execute(request, body);
    }
}
