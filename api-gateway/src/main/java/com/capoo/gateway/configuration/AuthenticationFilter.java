package com.capoo.gateway.configuration;

import com.capoo.gateway.dto.ApiResponse;
import com.capoo.gateway.service.IdentityService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationFilter implements GlobalFilter, Ordered {
    IdentityService identityService;
    ObjectMapper objectMapper;
    @NonFinal
    String[] publicEndpoints=new String[]{
            "/identity/auth/.*","/identity/users/registration",
            "/notification/.*",
            "/file/media/download/.*"

    };
    @Value("${app.api-prefix}")
    @NonFinal
    String apiPrefix;

    private static final String[] PUBLIC_ENDPOINTS = {
    };
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.info("AuthenticationFilter");
        //public endpoints
        if (isPublicEndpoint(exchange.getRequest())) {
            return chain.filter(exchange);
        }
        // Get Token from header
        List<String> authHeader=exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION);
        if (CollectionUtils.isEmpty(authHeader)) {
            return unAuthenticatedResponse(exchange.getResponse());
        }
        String token=authHeader.getFirst().substring("Bearer ".length());
        // Verify token
        return  identityService.introspectToken(token).flatMap(
                introspectResponseApiResponse -> {
                    if (introspectResponseApiResponse.getResult().isValid())
                        return chain.filter(exchange);
                    else
                        return unAuthenticatedResponse(exchange.getResponse());
                }).onErrorResume(throwable -> unAuthenticatedResponse(exchange.getResponse()));
    }
    @Override
    public int getOrder() {
        return -1;
    }
    private boolean isPublicEndpoint(ServerHttpRequest request) {
        return Arrays.stream(publicEndpoints)
                .anyMatch(s->request.getURI().getPath().matches(apiPrefix+s));
    }

    Mono<Void> unAuthenticatedResponse(ServerHttpResponse response) {
        ApiResponse<?> apiResponse=ApiResponse.builder()
                .code(HttpStatus.UNAUTHORIZED.value())
                .message("Unauthenticated")
                .build();
        String body;
        try {
            body=objectMapper.writeValueAsString(apiResponse);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        return response.writeWith(
                Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }

}
