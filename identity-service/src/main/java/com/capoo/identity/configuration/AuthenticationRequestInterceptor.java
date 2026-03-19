package com.capoo.identity.configuration;

import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.ServletRequestAttributes;
@Slf4j
@Component
public class AuthenticationRequestInterceptor implements RequestInterceptor {

    @Override
    public void apply(feign.RequestTemplate requestTemplate) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        var header= attributes.getRequest().getHeader("Authorization");
        if (StringUtils.hasText(header)) {
            log.info("Adding Authorization header to outgoing request: {}", header);
            requestTemplate.header("Authorization", header);
        }
    }
}
