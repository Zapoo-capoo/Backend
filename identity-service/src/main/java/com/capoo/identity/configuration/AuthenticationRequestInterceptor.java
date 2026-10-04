package com.capoo.identity.configuration;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AuthenticationRequestInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate requestTemplate) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        // No request context (e.g. called during application startup) -> nothing to propagate
        if (attributes == null) {
            return;
        }
        var header = attributes.getRequest().getHeader("Authorization");
        if (StringUtils.hasText(header)) {
            requestTemplate.header("Authorization", header);
        }
    }
}
